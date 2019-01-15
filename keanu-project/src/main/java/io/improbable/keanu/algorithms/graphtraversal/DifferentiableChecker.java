package io.improbable.keanu.algorithms.graphtraversal;

import io.improbable.keanu.vertices.ConstantVertex;
import io.improbable.keanu.vertices.Vertex;
import io.improbable.keanu.vertices.dbl.DoubleVertex;
import lombok.experimental.UtilityClass;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Utility class for checking whether the given vertices are all differentiable w.r.t latents.
 * When given latent variables, this ensures that the dLogProb can be calculated.
 * <p>
 * This check is performed by traversing up each vertex's parents and ensuring that the path to next RV is
 * differentiable or constant valued.
 * If there is a non differentiable vertex on this path, then if it is constant valued (0 gradient) it has no effect
 * and therefore will return true.
 *
 *
 * -- Examples --
 * RV = Random Variable
 * (G) = A vertex we want to check whether differentiable
 * ND = Non-differentiable vertex
 * D = Differentiable vertex
 * C = Constant valued vertex
 *
 * - Differentiable -
 *
 *      RV  RV
 *       \  /
 *        D    RV
 *         \  /
 *         RV(G)
 *
 * This graph is differentiable as traversing up each of the vertex's parent to the next RV is a differentiable path.
 *
 *      C    C
 *       \  /
 *        ND    RV
 *         \  /
 *         RV(G)
 *
 * This graph is differentiable as the path that is non differentiable is constant valued.
 *
 * - Not Differentiable -
 *
 *      RV  RV     - Both RV not observed
 *       \  /
 *        ND    RV
 *         \  /
 *         RV(G)
 *
 * This is not differentiable as there is a non differentiable path which does not have a constant value.
 *
 * For more examples see DifferentiableCheckerTest.java.
 */
@UtilityClass
public class DifferentiableChecker {

    /**
     * @param vertices the vertices to check are differentiable w.r.t latents.
     * @return true if all given vertices are differentiable, false otherwise.
     */
    public static boolean isDifferentiableWrtLatents(Collection<Vertex> vertices) {
        // All probabilistic need to be double or observed to ensure that the dLogProb can be calculated, for example
        // the dLogProb of BernoulliVertex can only be calculated when it is observed.
        if (!allProbabilisticAreDoubleOrObserved(vertices)) {
            return false;
        }
        Set<Vertex> allParents = allParentsOf(vertices);
        Set<Vertex> constantValueVerticesCache = new HashSet<>();
        return diffableOrConstantUptoNextRV(allParents, constantValueVerticesCache);
    }

    private static boolean allProbabilisticAreDoubleOrObserved(Collection<Vertex> vertices) {
        return vertices.stream().filter(Vertex::isProbabilistic)
            .allMatch(DifferentiableChecker::isDoubleOrObserved);
    }

    private static boolean isDoubleOrObserved(Vertex v) {
        return (v instanceof DoubleVertex || v.isObserved());
    }

    private static Set<Vertex> allParentsOf(Collection<Vertex> vertices) {
        Set<Vertex> allParents = new HashSet<>();
        for (Vertex vertex : vertices) {
            allParents.addAll(vertex.getParents());
        }
        return allParents;
    }

    private static boolean diffableOrConstantUptoNextRV(Collection<Vertex> vertices, Set<Vertex> constantValueVerticesCache) {
        return bfs(vertices,
            vertex -> isNonDiffableAndNotConstant(vertex, constantValueVerticesCache),
            vertex -> !vertex.isProbabilistic(),
            doNothing -> {
            });
    }

    private static boolean bfs(Collection<Vertex> vertices,
                               Predicate<Vertex> failureCondition,
                               Predicate<Vertex> shouldAddParents,
                               Consumer<Collection<Vertex>> successfullyVisitedConsumer) {

        Queue<Vertex> queue = new ArrayDeque<>(vertices);
        Set<Vertex> visited = new HashSet<>(vertices);

        while (!queue.isEmpty()) {
            Vertex visiting = queue.poll();

            if (failureCondition.test(visiting)) {
                return false;
            }

            if (shouldAddParents.test(visiting)) {
                queueUnvisitedParents(visiting, queue, visited);
            }
        }

        successfullyVisitedConsumer.accept(visited);
        return true;
    }

    private static void queueUnvisitedParents(Vertex vertex, Queue<Vertex> queue, Set<Vertex> visited) {
        Collection<Vertex> nextVertices = vertex.getParents();
        for (Vertex next : nextVertices) {
            if (!visited.contains(next)) {
                queue.offer(next);
                visited.add(next);
            }
        }
    }

    private static boolean isNonDiffableAndNotConstant(Vertex vertex, Set<Vertex> constantValueVerticesCache) {
        return !vertex.isDifferentiable() &&
            !isVertexValueConstant(vertex, constantValueVerticesCache);
    }

    private static boolean isVertexValueConstant(Vertex vertex, Set<Vertex> constantValueVerticesCache) {
        if (isValueKnownToBeConstant(vertex, constantValueVerticesCache)) {
            return true;
        }

        return bfs(Collections.singletonList(vertex),
            DifferentiableChecker::isUnobservedProbabilistic,
            visiting -> !isValueKnownToBeConstant(visiting, constantValueVerticesCache),
            constantValueVerticesCache::addAll);
    }

    private static boolean isUnobservedProbabilistic(Vertex vertex) {
        return vertex.isProbabilistic() && !vertex.isObserved();
    }

    // We know whether these are constant. For cases such as a MultiplicationVertex we would need to
    // explore its parents to ensure its constant.
    private static boolean isValueKnownToBeConstant(Vertex vertex, Set<Vertex> constantValueVerticesCache) {
        return vertex instanceof ConstantVertex || vertex.isObserved() || constantValueVerticesCache.contains(vertex);
    }
}
