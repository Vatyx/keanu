package io.improbable.keanu.vertices.dbl.nonprobabilistic.operators.unary;

import io.improbable.keanu.tensor.dbl.DoubleTensor;
import io.improbable.keanu.vertices.Vertex;
import io.improbable.keanu.vertices.VertexId;
import io.improbable.keanu.vertices.dbl.DoubleVertex;
import io.improbable.keanu.vertices.dbl.nonprobabilistic.diff.DualNumber;
import io.improbable.keanu.vertices.dbl.nonprobabilistic.diff.PartialDerivatives;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static io.improbable.keanu.tensor.TensorShape.copyLowRankOverHighRank;

public class ReshapeVertex extends DoubleUnaryOpVertex {

    public ReshapeVertex(DoubleVertex inputVertex, int... proposedShape) {
        super(proposedShape, inputVertex);
    }

    @Override
    protected DoubleTensor op(DoubleTensor value) {
        return value.reshape(getShape());
    }

    @Override
    protected DualNumber dualOp(DualNumber dualNumber) {
        return dualNumber.reshape(getShape());
    }

    @Override
    public Map<Vertex, PartialDerivatives> reverseModeAutoDifferentiation(PartialDerivatives derivativeOfOutputsWithRespectToSelf) {
        Map<Vertex, PartialDerivatives> reshapedDerivatives = new HashMap<>();

        for (Map.Entry<VertexId, DoubleTensor> partialDerivative : derivativeOfOutputsWithRespectToSelf.asMap().entrySet()) {
            int[] newPartialShape = copyLowRankOverHighRank(partialDerivative.getValue().getShape(), inputVertex.getShape());
            DoubleTensor reshapedPartialDerivative = partialDerivative.getValue().reshape(newPartialShape);
            reshapedDerivatives.put(inputVertex, new PartialDerivatives(partialDerivative.getKey(), reshapedPartialDerivative));
        }

        return reshapedDerivatives;
    }

}
