package io.improbable.keanu.vertices.dbl.nonprobabilistic.diff;

import static java.util.Collections.singletonMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.improbable.keanu.tensor.Tensor;
import io.improbable.keanu.tensor.TensorShape;
import io.improbable.keanu.tensor.bool.BooleanTensor;
import io.improbable.keanu.tensor.dbl.DoubleTensor;
import io.improbable.keanu.vertices.Vertex;
import io.improbable.keanu.vertices.VertexId;

public class PartialDerivatives {

    public static PartialDerivatives OF_CONSTANT = new PartialDerivatives(Collections.emptyMap());

    public static PartialDerivatives withRespectToSelf(VertexId withRespectTo, int[] shape) {
        return new PartialDerivatives(
            singletonMap(
                withRespectTo,
                DoubleTensor.eye((int) TensorShape.getLength(shape)).reshape(TensorShape.concat(shape, shape))
            )
        );
    }

    public static PartialDerivatives ifThenElse(BooleanTensor predicate, PartialDerivatives thn, PartialDerivatives els) {
        DoubleTensor trueMask = predicate.toDoubleMask();
        DoubleTensor falseMask = predicate.not().toDoubleMask();

        Map<VertexId, DoubleTensor> thenPartials = thn.derivativeWithRespectTo;
        Map<VertexId, DoubleTensor> elsePartials = els.derivativeWithRespectTo;
        Set<VertexId> wrtUnion = new HashSet<>();
        wrtUnion.addAll(thenPartials.keySet());
        wrtUnion.addAll(elsePartials.keySet());

        Map<VertexId, DoubleTensor> mixedPartials = new HashMap<>();
        for (VertexId wrt : wrtUnion) {
            DoubleTensor thnPartial = thenPartials.get(wrt);
            DoubleTensor elsPartial = elsePartials.get(wrt);
            DoubleTensor broadcastedTrueMask;
            DoubleTensor broadcastedFalseMask;
            int[] range = TensorShape.dimensionRange(0, thnPartial == null ? elsPartial.getRank() : thnPartial.getRank());
            int lengthOfWrt = range.length / 2;
            int[] permute = TensorShape.concat(
                Arrays.copyOfRange(range, range.length - lengthOfWrt, range.length),
                Arrays.copyOfRange(range, 0, range.length - lengthOfWrt)
            );

            DoubleTensor newPartial;
            if (thnPartial == null) {
                broadcastedFalseMask = DoubleTensor.zeros(elsPartial.getShape()).plusInPlace(falseMask).permute(permute);
                newPartial = broadcastedFalseMask.timesInPlace(elsPartial);
            } else if (elsPartial == null) {
                broadcastedTrueMask = DoubleTensor.zeros(thnPartial.getShape()).plusInPlace(trueMask).permute(permute);
                newPartial = broadcastedTrueMask.timesInPlace(thnPartial);
            } else {
                broadcastedFalseMask = DoubleTensor.zeros(thnPartial.getShape()).plusInPlace(falseMask).permute(permute);
                broadcastedTrueMask = DoubleTensor.zeros(thnPartial.getShape()).plusInPlace(trueMask).permute(permute);

                newPartial = broadcastedTrueMask.timesInPlace(thnPartial)
                    .plusInPlace(broadcastedFalseMask.timesInPlace(elsPartial));
            }

            mixedPartials.put(wrt, newPartial);
        }

        return new PartialDerivatives(mixedPartials);
    }

    private Map<VertexId, DoubleTensor> derivativeWithRespectTo;

    public PartialDerivatives(VertexId id, DoubleTensor derivativeWithRespectTo) {
        this.derivativeWithRespectTo = new HashMap<>();
        this.derivativeWithRespectTo.put(id, derivativeWithRespectTo);
    }

    public PartialDerivatives(Map<VertexId, DoubleTensor> derivativeWithRespectTo) {
        this.derivativeWithRespectTo = derivativeWithRespectTo;
    }

    public DoubleTensor withRespectTo(Vertex vertex) {
        return withRespectTo(vertex.getId());
    }

    public DoubleTensor withRespectTo(VertexId id) {
        return derivativeWithRespectTo.getOrDefault(id, DoubleTensor.ZERO_SCALAR);
    }

    public boolean isEmpty() {
        return derivativeWithRespectTo.isEmpty();
    }

    public Map<VertexId, DoubleTensor> asMap() {
        return derivativeWithRespectTo;
    }

    public void putWithRespectTo(VertexId id, DoubleTensor value) {
        derivativeWithRespectTo.put(id, value);
    }

    /**
     * This will sum partial derivatives that are represented as tensors over given dimensions.
     * There is the option to reshape to a lower rank tensor where the summation has caused a
     * dimension to go to length 1.
     *
     * @param reshape        Returns the sum and drops the summed over dimensions (now length one)
     *                       in the shape if true. Returns a same ranked tensor but with a shape
     *                       that has ones for the dimensions summed over.
     * @param overDimensions The dimensions to sum over. Dimensions are counted from zero
     * @return The summed partial derivatives over given dimensions
     */
    public PartialDerivatives sum(boolean reshape, int... overDimensions) {
        Map<VertexId, DoubleTensor> summed = cloneInfinitesimals(derivativeWithRespectTo);

        for (Map.Entry<VertexId, DoubleTensor> entry : derivativeWithRespectTo.entrySet()) {
            VertexId k = entry.getKey();
            DoubleTensor v = entry.getValue();
            DoubleTensor reshapedV = v.sum(overDimensions);
            if (reshape) {
                summed.put(k, reshapedV);
            } else {
                summed.put(k, reshapedV.reshape(onesToShape(v.getShape(), overDimensions)));
            }
        }

        return new PartialDerivatives(summed);
    }

    public PartialDerivatives add(PartialDerivatives toAdd) {
        return add(toAdd, null);
    }

    public PartialDerivatives add(PartialDerivatives addition, int[] ofShape) {

        Map<VertexId, DoubleTensor> added = cloneWithCorrectShape(derivativeWithRespectTo, ofShape);
        Map<VertexId, DoubleTensor> toAdd = cloneWithCorrectShape(addition.derivativeWithRespectTo, ofShape);

        for (Map.Entry<VertexId, DoubleTensor> entry : toAdd.entrySet()) {
            VertexId k = entry.getKey();
            DoubleTensor v = entry.getValue();

            if (added.containsKey(k)) {
                added.put(k, added.get(k).plus(v));
            } else {
                added.put(k, v);
            }
        }

        return new PartialDerivatives(added);
    }

    public PartialDerivatives subtract(PartialDerivatives subtraction) {
        return subtract(subtraction, null);
    }

    public PartialDerivatives subtract(PartialDerivatives subtraction, int[] ofShape) {

        Map<VertexId, DoubleTensor> subtracted = cloneWithCorrectShape(derivativeWithRespectTo, ofShape);
        Map<VertexId, DoubleTensor> toSubtract = cloneWithCorrectShape(subtraction.derivativeWithRespectTo, ofShape);

        for (Map.Entry<VertexId, DoubleTensor> entry : toSubtract.entrySet()) {
            VertexId k = entry.getKey();
            DoubleTensor v = entry.getValue();

            if (subtracted.containsKey(k)) {
                subtracted.put(k, subtracted.get(k).minus(v));
            } else {
                subtracted.put(k, v.unaryMinus());
            }
        }

        return new PartialDerivatives(subtracted);
    }

    private static Map<VertexId, DoubleTensor> cloneWithCorrectShape(Map<VertexId, DoubleTensor> infinitesimals, int[] ofShape) {

        Map<VertexId, DoubleTensor> clone = new HashMap<>();
        for (Map.Entry<VertexId, DoubleTensor> entry : infinitesimals.entrySet()) {
            VertexId k = entry.getKey();
            DoubleTensor v = entry.getValue();

            if (ofShape == null || ofShapeMatches(ofShape, v.getShape())) {
                clone.put(k, v);
            } else {
                clone.put(k, DoubleTensor.zeros(shapeWrtScalar(ofShape, v.getShape())).plus(v));
            }
        }
        return clone;
    }

    private static boolean ofShapeMatches(int[] ofShape, int[] partialShape) {
        for (int i = 0; i < ofShape.length; i++) {
            if (ofShape[i] != partialShape[i]) {
                return false;
            }
        }
        return true;
    }

    private static int[] shapeWrtScalar(int[] ofShape, int[] partialShape) {
        int[] fixedShape = Arrays.copyOf(partialShape, partialShape.length);
        System.arraycopy(ofShape, 0, fixedShape, 0, ofShape.length);
        return fixedShape;
    }

    public PartialDerivatives multiplyBy(DoubleTensor multiplier) {
        return multiplyBy(multiplier, false);
    }

    public PartialDerivatives multiplyBy(DoubleTensor multiplier, boolean alongWrtDimensions) {
        Map<VertexId, DoubleTensor> multiplied = new HashMap<>();

        for (Map.Entry<VertexId, DoubleTensor> entry : derivativeWithRespectTo.entrySet()) {
            VertexId k = entry.getKey();
            DoubleTensor partial = entry.getValue();
            DoubleTensor result;

            if (multiplier.isScalar()) {
                result = partial.times(multiplier.scalar());
            } else if (alongWrtDimensions) {
                result = elementWiseMultiplyAlongWrt(partial, multiplier);
            } else {
                result = elementWiseMultiplyAlongOf(partial, multiplier);
            }

            multiplied.put(k, result);
        }

        return new PartialDerivatives(multiplied);
    }

    private DoubleTensor elementWiseMultiplyAlongOf(DoubleTensor partial, DoubleTensor multiplier) {

        DoubleTensor multiplierFromLeft = increaseRankByAppendingOnesToShape(multiplier, partial.getRank());

        int[] partialOfShape = extractOfShape(partial.getShape(), multiplier.getRank());
        if (TensorShape.isScalar(partialOfShape)) {

            int[] partialWrtShape = extractWrtShape(partial.getShape(), multiplier.getRank());
            int[] resultShape = TensorShape.concat(multiplier.getShape(), partialWrtShape);

            return DoubleTensor.ones(resultShape).times(partial)
                .times(multiplierFromLeft);
        }
                return partial.times(multiplierFromLeft);
    }

    private DoubleTensor elementWiseMultiplyAlongWrt(DoubleTensorpartial, DoubleTensor multiplier){
        int[] partialWrtShape = extractWrtShape(partial.getShape(), multiplier.getRank());
        if (TensorShape.isScalar(partialWrtShape)) {

            int[] partialOfShape = extractOfShape(partial.getShape(), multiplier.getRank());
            int[] resultShape = TensorShape.concat(partialOfShape, multiplier.getShape());
            return DoubleTensor.ones(resultShape).times(partial).times(multiplier);
        }

        return partial.times(multiplier);
    }

    public static PartialDerivatives matrixMultiplyAlongOfDimensions(PartialDerivatives partials, DoubleTensor multiplier, boolean partialIsLeft) {
        Map<VertexId, DoubleTensor> multiplied = new HashMap<>();

        for (Map.Entry<VertexId, DoubleTensor> partial : partials.derivativeWithRespectTo.entrySet()) {

            int partialRank = partial.getValue().getRank();

            DoubleTensor result;
            if (partialIsLeft) {
                int[] rearrange = TensorShape.dimensionRange(-1, partialRank - 1);
                rearrange[0] = 0;
                rearrange[1] = partialRank - 1;
                result = partial.getValue()
                    .tensorMultiply(multiplier, new int[]{1}, new int[]{0})
                    .permute(rearrange);

            } else {
                result = multiplier
                    .tensorMultiply(partial.getValue(), new int[]{1}, new int[]{0});
            }
            multiplied.put(partial.getKey(), result);
        }

        return new PartialDerivatives(multiplied);
    }

    public static PartialDerivatives matrixMultiplyAlongWrtDimensions(PartialDerivatives partials, DoubleTensor multiplier, boolean partialIsLeft) {
        Map<VertexId, DoubleTensor> multiplied = new HashMap<>();

        for (Map.Entry<VertexId, DoubleTensor> partial : partials.derivativeWithRespectTo.entrySet()) {

            int partialRank = partial.getValue().getRank();

            int wrtRightDimension = partialRank - 1;
            int wrtLeftDimension = partialRank - 2;

            DoubleTensor result;
            if (partialIsLeft) {
                result = partial.getValue()
                    .tensorMultiply(multiplier, new int[]{wrtRightDimension}, new int[]{1});
            } else {
                int[] transposeWrt = TensorShape.dimensionRange(0, partialRank);
                transposeWrt[wrtRightDimension] = wrtLeftDimension;
                transposeWrt[wrtLeftDimension] = wrtRightDimension;

                result = partial.getValue()
                    .tensorMultiply(multiplier, new int[]{wrtLeftDimension}, new int[]{0})
                    .permute(transposeWrt);
            }
            multiplied.put(partial.getKey(), result);
        }

        return new PartialDerivatives(multiplied);
    }

    public PartialDerivatives multiplyBy(double multiplier) {
        Map<VertexId, DoubleTensor> multiplied = new HashMap<>();

        for (Map.Entry<VertexId, DoubleTensor> entry : derivativeWithRespectTo.entrySet()) {
            VertexId k = entry.getKey();
            DoubleTensor v = entry.getValue().times(multiplier);
            multiplied.put(k, v);
        }

        return new PartialDerivatives(multiplied);
    }

    public PartialDerivatives divideBy(DoubleTensor divisor) {
        Map<VertexId, DoubleTensor> divided = new HashMap<>();

        for (Map.Entry<VertexId, DoubleTensor> entry : derivativeWithRespectTo.entrySet()) {
            VertexId k = entry.getKey();
            DoubleTensor partial = entry.getValue();
            DoubleTensor v = partial.div(increaseRankByAppendingOnesToShape(divisor, partial.getRank()));
            divided.put(k, v);
        }

        return new PartialDerivatives(divided);
    }

    public PartialDerivatives divideBy(double divisor) {
        Map<VertexId, DoubleTensor> divided = new HashMap<>();

        for (Map.Entry<VertexId, DoubleTensor> entry : derivativeWithRespectTo.entrySet()) {
            VertexId k = entry.getKey();
            DoubleTensor v = entry.getValue().div(divisor);
            divided.put(k, v);
        }

        return new PartialDerivatives(divided);
    }

    public PartialDerivatives clone() {
        return new PartialDerivatives(cloneInfinitesimals(derivativeWithRespectTo));
    }

    public PartialDerivatives reshape(int currentRank, int[] proposedShape) {
        Map<VertexId, DoubleTensor> reshapedDerivatives = new HashMap<>();

        for (Map.Entry<VertexId, DoubleTensor> partialDerivative : derivativeWithRespectTo.entrySet()) {
            int[] shape = partialDerivative.getValue().getShape();
            int[] wrtShape = extractWrtShape(shape, currentRank);
            int[] newPartialShape = TensorShape.concat(proposedShape, wrtShape);

            DoubleTensor reshapedPartialDerivative = partialDerivative.getValue().reshape(newPartialShape);
            reshapedDerivatives.put(partialDerivative.getKey(), reshapedPartialDerivative);
        }

        return new PartialDerivatives(reshapedDerivatives);
    }

    public PartialDerivatives slice(int dimension, int index) {
        Map<VertexId, DoubleTensor> slicedDerivatives = new HashMap<>();

        for (Map.Entry<VertexId, DoubleTensor> partialDerivative : derivativeWithRespectTo.entrySet()) {
            int[] partialDerivativeShape = partialDerivative.getValue().getShape();
            partialDerivativeShape[dimension] = 1;
            DoubleTensor slicedPartialDerivative = partialDerivative.getValue().slice(dimension, index);
            slicedPartialDerivative = slicedPartialDerivative.reshape(partialDerivativeShape);
            slicedDerivatives.put(partialDerivative.getKey(), slicedPartialDerivative);
        }

        return new PartialDerivatives(slicedDerivatives);
    }

    private static Map<VertexId, DoubleTensor> cloneInfinitesimals(Map<VertexId, DoubleTensor> infinitesimals) {
        Map<VertexId, DoubleTensor> clone = new HashMap<>();
        for (Map.Entry<VertexId, DoubleTensor> entry : infinitesimals.entrySet()) {
            clone.put(entry.getKey(), entry.getValue());
        }
        return clone;
    }

    private int[] extractWrtShape(int[] partialDerivativeShape, int rankOfSource) {
        return Arrays.copyOfRange(partialDerivativeShape, rankOfSource, partialDerivativeShape.length);
    }

    private int[] extractOfShape(int[] partialDerivativeShape, int rankOfSource) {
        return Arrays.copyOfRange(partialDerivativeShape, 0, rankOfSource);
    }

    private static int[] onesToShape(int[] shape, int[] onesDimensions) {

        int[] shapeWithOnes = Arrays.copyOf(shape, shape.length);

        for (int onesDimension : onesDimensions) {
            int resolvedDimension = onesDimension >= 0 ? onesDimension : shape.length + onesDimension;
            shapeWithOnes[resolvedDimension] = 1;
        }

        return shapeWithOnes;
    }

    private static DoubleTensor increaseRankByAppendingOnesToShape(DoubleTensor lowRankTensor, int desiredRank) {
        return lowRankTensor.reshape(
            TensorShape.shapeDesiredToRankByAppendingOnes(lowRankTensor.getShape(), desiredRank)
        );
    }

}
