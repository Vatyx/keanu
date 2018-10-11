package io.improbable.keanu.vertices.dbl.nonprobabilistic.operators.binary;

import static io.improbable.keanu.vertices.dbl.nonprobabilistic.operators.TensorTestOperations.finiteDifferenceMatchesForwardAndReverseModeGradient;
import static io.improbable.keanu.vertices.dbl.nonprobabilistic.operators.binary.BinaryOperationTestHelpers.*;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.improbable.keanu.tensor.dbl.DoubleTensor;
import io.improbable.keanu.vertices.dbl.DoubleVertex;
import io.improbable.keanu.vertices.dbl.probabilistic.UniformVertex;

public class DivisionVertexTest {

    @Test
    public void dividesTwoScalarVertexValues() {
        operatesOnTwoScalarVertexValues(
            12.0,
            3.0,
            4.0,
            DoubleVertex::divideBy
        );
    }

    @Test
    public void calculatesDerivativeOfTwoScalarsDivided() {
        calculatesDerivativeOfTwoScalars(
            2.0,
            3.0,
            1.0 / 3.0,
            -2.0 / 9.0,
            DoubleVertex::divideBy
        );
    }

    @Test
    public void dividesTwoMatrixVertexValues() {
        operatesOnTwo2x2MatrixVertexValues(
            new double[]{1.0, 2.0, 6.0, 4.0},
            new double[]{2.0, 4.0, 3.0, 8.0},
            new double[]{1.0 / 2.0, 2.0 / 4.0, 6.0 / 3.0, 4.0 / 8.0},
            DoubleVertex::divideBy
        );
    }

    @Test
    public void calculatesDerivativeOfTwoMatricesElementWiseDivided() {
        calculatesDerivativeOfTwoMatricesElementWiseOperator(
            DoubleTensor.create(new double[]{1.0, 2.0, 3.0, 4.0}, 1, 4),
            DoubleTensor.create(new double[]{2.0, 3.0, 4.0, 5.0}, 1, 4),
            DoubleTensor.create(new double[]{1.0 / 2.0, 1.0 / 3.0, 1.0 / 4.0, 1.0 / 5.0}).diag().reshape(1, 4, 1, 4),
            DoubleTensor.create(new double[]{-1.0 / 4.0, -2.0 / 9.0, -3.0 / 16.0, -4.0 / 25.0}).diag().reshape(1, 4, 1, 4),
            DoubleVertex::divideBy
        );
    }

    @Test
    public void calculatesDerivativeOfAVectorsAndScalarMultiplied() {
        calculatesDerivativeOfAVectorAndScalar(
            DoubleTensor.create(new double[]{1.0, 2.0, 3.0, 4.0}),
            2,
            DoubleTensor.eye(4).div(2).reshape(1, 4, 1, 4),
            DoubleTensor.create(new double[]{-0.25, -0.5, -0.75, -1.0}, 1, 4, 1, 1),
            DoubleVertex::divideBy
        );
    }

    @Test
    public void calculatesDerivativeofAScalarAndVectorsMultiplied() {
        calculatesDerivativeOfAScalarAndVector(
            2,
            DoubleTensor.create(new double[]{1.0, 2.0, 3.0, 4.0}),
            DoubleTensor.create(new double[]{1. / 1., 1. / 2., 1. / 3., 1. / 4.}, 1, 4, 1, 1),
            DoubleTensor.create(new double[]{-2.0 / 1.0, -2.0 / 4.0, -2.0 / 9.0, -2.0 / 16.}).diag().reshape(1, 4, 1, 4),
            DoubleVertex::divideBy
        );
    }

    @Test
    public void changesMatchGradient() {
        DoubleVertex A = new UniformVertex(new long[]{2, 2, 2}, 1.0, 10.0);
        DoubleVertex B = new UniformVertex(new long[]{2, 2, 2}, 100.0, 150.0);
        DoubleVertex C = A.div(B).times(A);

        finiteDifferenceMatchesForwardAndReverseModeGradient(ImmutableList.of(A, B), C, 0.001, 1e-5);
    }
}
