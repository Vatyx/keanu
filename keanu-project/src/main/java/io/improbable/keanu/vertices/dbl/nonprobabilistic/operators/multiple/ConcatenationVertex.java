package io.improbable.keanu.vertices.dbl.nonprobabilistic.operators.multiple;

import io.improbable.keanu.tensor.TensorShape;
import io.improbable.keanu.tensor.dbl.DoubleTensor;
import io.improbable.keanu.vertices.Vertex;
import io.improbable.keanu.vertices.dbl.DoubleVertex;
import io.improbable.keanu.vertices.dbl.KeanuRandom;
import io.improbable.keanu.vertices.dbl.nonprobabilistic.NonProbabilisticDouble;
import io.improbable.keanu.vertices.dbl.nonprobabilistic.diff.DualNumber;
import io.improbable.keanu.vertices.dbl.nonprobabilistic.diff.PartialDerivatives;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.improbable.keanu.tensor.TensorShapeValidation.checkShapesCanBeConcatenated;

public class ConcatenationVertex extends NonProbabilisticDouble {

    private final int dimension;
    private final DoubleVertex[] input;

    /**
     * A vertex that can concatenate any amount of vertices along a given dimension.
     *
     * @param dimension the dimension to concatenate on. This is the only dimension in which sizes may be different.
     * @param input the input vertices to concatenate
     */
    public ConcatenationVertex(int dimension, DoubleVertex... input) {
        this.dimension = dimension;
        this.input = input;
        setParents(input);
        int[][] shapes = extractFromInputs(int[].class, Vertex::getShape);
        setValue(DoubleTensor.placeHolder(checkShapesCanBeConcatenated(dimension, shapes)));
    }

    @Override
    public DoubleTensor getDerivedValue() {
        return op(extractFromInputs(DoubleTensor.class, Vertex::getValue));
    }

    @Override
    protected DualNumber calculateDualNumber(Map<Vertex, DualNumber> dualNumbers) {
        List<DualNumber> duals = new ArrayList<>();

        for (DoubleVertex vertex : input) {
            duals.add(dualNumbers.get(vertex));
        }
        DoubleTensor[] inputValues = extractFromInputs(DoubleTensor.class, Vertex::getValue);
        return DualNumber.concat(dualNumbers, duals, input, dimension, inputValues);
    }

    @Override
    protected Map<Vertex, PartialDerivatives> reverseModeAutoDifferentiation(PartialDerivatives derivativeOfOutputsWithRespectToSelf) {
        DoubleTensor value = derivativeOfOutputsWithRespectToSelf.asMap().get(this.getId());
        int[] partialShape = value.getShape();
        int[] rearrange = TensorShape.dimensionRange(0, partialShape.length);
        rearrange[dimension] = 0;
        rearrange[0] = dimension;

        DoubleTensor permuted = value.permute(rearrange);
        double[] permutedBuffer = permuted.asFlatDoubleArray();

        Map<Vertex, PartialDerivatives> concattedPartial = new HashMap<>();

        int bufferOffset = 0;
        for (DoubleVertex vertex : input) {
            int[] ofWrtShape = TensorShape.concat(Arrays.copyOfRange(value.getShape(), 0, vertex.getValue().getRank()), vertex.getShape());
            int inputSize = (int) (value.getLength() / (value.getShape()[value.getShape().length / 2 + dimension])) * vertex.getShape()[dimension];
            double[] inputsDualNumbers = Arrays.copyOfRange(permutedBuffer, bufferOffset, bufferOffset + inputSize);
            DoubleTensor unpermuted = DoubleTensor.create(inputsDualNumbers, ofWrtShape).permute(rearrange);
            PartialDerivatives partial = new PartialDerivatives(getId(), unpermuted);
            concattedPartial.put(vertex, partial);
            bufferOffset += inputSize;
        }

        return concattedPartial;
    }

    @Override
    public DoubleTensor sample(KeanuRandom random) {
        return op(extractFromInputs(DoubleTensor.class, Vertex::sample));
    }

    protected DoubleTensor op(DoubleTensor... inputs) {
        DoubleTensor primary = inputs[0];
        DoubleTensor[] toConcat = Arrays.copyOfRange(inputs, 1, inputs.length);
        return primary.concat(dimension, toConcat);
    }

    private <T> T[] extractFromInputs(Class<T> clazz, Function<Vertex<DoubleTensor>, T> func) {
        T[] extract = (T[]) Array.newInstance(clazz, input.length);
        for (int i = 0; i < input.length; i++) {
            extract[i] = func.apply(input[i]);
        }
        return extract;
    }


}
