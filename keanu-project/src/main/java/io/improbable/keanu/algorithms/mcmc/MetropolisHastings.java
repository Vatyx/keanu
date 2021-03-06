package io.improbable.keanu.algorithms.mcmc;

import io.improbable.keanu.algorithms.NetworkSample;
import io.improbable.keanu.algorithms.PosteriorSamplingAlgorithm;
import io.improbable.keanu.algorithms.mcmc.proposal.MHStepVariableSelector;
import io.improbable.keanu.algorithms.mcmc.proposal.ProposalDistribution;
import io.improbable.keanu.network.BayesianNetwork;
import io.improbable.keanu.util.ProgressBar;
import io.improbable.keanu.vertices.Vertex;
import io.improbable.keanu.vertices.VertexId;
import io.improbable.keanu.vertices.dbl.KeanuRandom;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.improbable.keanu.algorithms.mcmc.SamplingAlgorithm.takeSample;
import static io.improbable.keanu.algorithms.mcmc.proposal.MHStepVariableSelector.SINGLE_VARIABLE_SELECTOR;

/**
 * Metropolis Hastings is a Markov Chain Monte Carlo method for obtaining samples from a probability distribution
 */
@Builder
@Slf4j
public class MetropolisHastings implements PosteriorSamplingAlgorithm {

    private static final ProposalDistribution DEFAULT_PROPOSAL_DISTRIBUTION = ProposalDistribution.usePrior();
    private static final MHStepVariableSelector DEFAULT_VARIABLE_SELECTOR = SINGLE_VARIABLE_SELECTOR;
    private static final boolean DEFAULT_USE_CACHE_ON_REJECTION = true;

    public static MetropolisHastings withDefaultConfig() {
        return withDefaultConfig(KeanuRandom.getDefaultRandom());
    }

    public static MetropolisHastings withDefaultConfig(KeanuRandom random) {
        return MetropolisHastings.builder()
            .random(random)
            .build();
    }

    @Getter
    @Setter
    @Builder.Default
    private KeanuRandom random = KeanuRandom.getDefaultRandom();

    @Getter
    @Setter
    @Builder.Default
    private ProposalDistribution proposalDistribution = DEFAULT_PROPOSAL_DISTRIBUTION;

    @Getter
    @Setter
    @Builder.Default
    private MHStepVariableSelector variableSelector = DEFAULT_VARIABLE_SELECTOR;

    @Getter
    @Setter
    @Builder.Default
    private boolean useCacheOnRejection = DEFAULT_USE_CACHE_ON_REJECTION;

    @Override
    public NetworkSamplesGenerator generatePosteriorSamples(final BayesianNetwork bayesianNetwork,
                                                            final List<? extends Vertex> verticesToSampleFrom) {

        return new NetworkSamplesGenerator(setupSampler(bayesianNetwork, verticesToSampleFrom), ProgressBar::new);
    }

    private SamplingAlgorithm setupSampler(final BayesianNetwork bayesianNetwork,
                                           final List<? extends Vertex> verticesToSampleFrom) {
        checkBayesNetInHealthyState(bayesianNetwork);

        List<Vertex> latentVertices = bayesianNetwork.getLatentVertices();

        MetropolisHastingsStep mhStep = new MetropolisHastingsStep(
            latentVertices,
            proposalDistribution,
            useCacheOnRejection,
            random
        );

        double logProbabilityBeforeStep = bayesianNetwork.getLogOfMasterP();

        return new MetropolisHastingsSampler(latentVertices, verticesToSampleFrom, mhStep, variableSelector, logProbabilityBeforeStep);
    }

    public static class MetropolisHastingsSampler implements SamplingAlgorithm {

        private final List<Vertex> latentVertices;
        private final List<? extends Vertex> verticesToSampleFrom;
        private final MetropolisHastingsStep mhStep;
        private final MHStepVariableSelector variableSelector;

        private double logProbabilityBeforeStep;
        private int sampleNum;

        public MetropolisHastingsSampler(List<Vertex> latentVertices,
                                         List<? extends Vertex> verticesToSampleFrom,
                                         MetropolisHastingsStep mhStep,
                                         MHStepVariableSelector variableSelector,
                                         double logProbabilityBeforeStep) {
            this.latentVertices = latentVertices;
            this.verticesToSampleFrom = verticesToSampleFrom;
            this.mhStep = mhStep;
            this.variableSelector = variableSelector;
            this.logProbabilityBeforeStep = logProbabilityBeforeStep;
            this.sampleNum = 0;
        }

        @Override
        public void step() {
            Set<Vertex> chosenVertices = variableSelector.select(latentVertices, sampleNum);

            logProbabilityBeforeStep = mhStep.step(
                chosenVertices,
                logProbabilityBeforeStep
            ).getLogProbabilityAfterStep();

            sampleNum++;
        }

        @Override
        public void sample(Map<VertexId, List<?>> samplesByVertex, List<Double> logOfMasterPForEachSample) {
            step();
            takeSamples(samplesByVertex, verticesToSampleFrom);
            logOfMasterPForEachSample.add(logProbabilityBeforeStep);
        }

        @Override
        public NetworkSample sample() {
            step();
            return new NetworkSample(takeSample(verticesToSampleFrom), logProbabilityBeforeStep);
        }
    }

    private static void takeSamples(Map<VertexId, List<?>> samples, List<? extends Vertex> fromVertices) {
        fromVertices.forEach(vertex -> addSampleForVertex((Vertex<?>) vertex, samples));
    }

    private static <T> void addSampleForVertex(Vertex<T> vertex, Map<VertexId, List<?>> samples) {
        List<T> samplesForVertex = (List<T>) samples.computeIfAbsent(vertex.getId(), v -> new ArrayList<T>());
        T value = vertex.getValue();
        samplesForVertex.add(value);
        log.trace(String.format("Sampled %s", value));
    }

    private static void checkBayesNetInHealthyState(BayesianNetwork bayesNet) {
        bayesNet.cascadeObservations();
        if (bayesNet.getLatentOrObservedVertices().isEmpty()) {
            throw new IllegalArgumentException("Cannot sample from a completely deterministic BayesNet");
        } else if (bayesNet.isInImpossibleState()) {
            throw new IllegalArgumentException("Cannot start optimizer on zero probability network");
        }
    }

}
