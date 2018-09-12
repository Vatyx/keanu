package io.improbable.keanu.vertices.dbl.nonprobabilistic.operators.multiple;

import io.improbable.keanu.vertices.NonProbabilistic;
import io.improbable.keanu.vertices.VertexLabel;
import io.improbable.keanu.vertices.dbl.DoubleVertex;

import java.util.Map;
import java.util.function.Function;

public interface ModelVertex<T> extends NonProbabilistic<T> {

    Double getModelOutputValue(VertexLabel label);

    DoubleVertex getModelOutputVertex(VertexLabel label);

}
