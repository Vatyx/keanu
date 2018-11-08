from keanu.context import KeanuContext
from keanu.vertex import Vertex
from keanu.vertex.vertex_label import VertexLabel
from keanu.tensor import Tensor
from keanu.functional import Consumer, Function
from keanu.vertex.base import JavaObjectWrapper
from py4j.java_gateway import java_import


context = KeanuContext()
java_import(context.jvm_view(), "io.improbable.keanu.vertices.model.LambdaModelVertex")

def _extract_values(vertices):
    value_map = {k: JavaObjectWrapper(v.getValue()) for k, v in vertices.items()}
    return context.to_java_map(value_map)

class LambdaModel(Vertex):
    def __init__(self, 
    inputs: map, 
    executor: Consumer, 
    update_value: Function = Function(_extract_values)
    ) -> Vertex:
        self.map = map
        self.executor = executor
        self.update_value = update_value
        val = context.jvm_view().LambdaModelVertex(inputs, executor, update_value)
        super(LambdaModel, self).__init__(val)

    def get_double_model_output_vertex(self, label : str):
        label = VertexLabel(label)
        result = self.unwrap().getDoubleModelOutputVertex(label.unwrap())
        return Vertex(result)


