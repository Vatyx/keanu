package io.improbable.keanu.vertices.dbl.nonprobabilistic.operators.unary;

import java.util.Map;
import java.util.function.Function;

import io.improbable.keanu.tensor.dbl.DoubleTensor;
import io.improbable.keanu.vertices.NonProbabilistic;
import io.improbable.keanu.vertices.Vertex;
import io.improbable.keanu.vertices.dbl.Differentiable;
import io.improbable.keanu.vertices.dbl.DoubleVertex;
import io.improbable.keanu.vertices.dbl.KeanuRandom;
import io.improbable.keanu.vertices.dbl.nonprobabilistic.diff.DualNumber;

public class DoubleUnaryOpLambda<IN> extends DoubleVertex implements Differentiable, NonProbabilistic<DoubleTensor> {

    private final Vertex<IN> inputVertex;
    private final Function<IN, DoubleTensor> op;
    private final Function<Map<Vertex, DualNumber>, DualNumber> dualNumberSupplier;

    public DoubleUnaryOpLambda(int[] shape, Vertex<IN> inputVertex, Function<IN, DoubleTensor> op, Function<Map<Vertex, DualNumber>, DualNumber> dualNumberCalculation) {
        this.inputVertex = inputVertex;
        this.op = op;
        this.dualNumberSupplier = dualNumberCalculation;
        setParents(inputVertex);
        setValue(DoubleTensor.placeHolder(shape));
    }

    public DoubleUnaryOpLambda(int[] shape, Vertex<IN> inputVertex, Function<IN, DoubleTensor> op) {
        this(shape, inputVertex, op, null);
    }

    public DoubleUnaryOpLambda(Vertex<IN> inputVertex, Function<IN, DoubleTensor> op, Function<Map<Vertex, DualNumber>, DualNumber> dualNumberCalculation) {
        this(inputVertex.getShape(), inputVertex, op, dualNumberCalculation);
    }

    public DoubleUnaryOpLambda(Vertex<IN> inputVertex, Function<IN, DoubleTensor> op) {
        this(inputVertex.getShape(), inputVertex, op, null);
    }

    @Override
    public DoubleTensor sample(KeanuRandom random) {
        return op.apply(inputVertex.sample(random));
    }

    @Override
    public DoubleTensor calculate() {
        return op.apply(inputVertex.getValue());
    }

    @Override
    public DualNumber calculateDualNumber(Map<Vertex, DualNumber> dualNumbers) {
        if (dualNumberSupplier != null) {
            return dualNumberSupplier.apply(dualNumbers);
        }

        throw new UnsupportedOperationException();
    }
}
