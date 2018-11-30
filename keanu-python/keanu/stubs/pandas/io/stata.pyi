# Stubs for pandas.io.stata (Python 3.6)
#
# NOTE: This dynamically typed stub was automatically generated by stubgen.

from pandas.core.base import StringMixin
from pandas.io.common import BaseIterator
from typing import Any, Optional

def read_stata(filepath_or_buffer: Any, convert_dates: bool = ..., convert_categoricals: bool = ..., encoding: Optional[Any] = ..., index_col: Optional[Any] = ..., convert_missing: bool = ..., preserve_dtypes: bool = ..., columns: Optional[Any] = ..., order_categoricals: bool = ..., chunksize: Optional[Any] = ..., iterator: bool = ...): ...

stata_epoch: Any
excessive_string_length_error: str

class PossiblePrecisionLoss(Warning): ...

precision_loss_doc: str

class ValueLabelTypeMismatch(Warning): ...

value_label_mismatch_doc: str

class InvalidColumnName(Warning): ...

invalid_name_doc: str

class StataValueLabel:
    labname: Any = ...
    value_labels: Any = ...
    text_len: Any = ...
    off: Any = ...
    val: Any = ...
    txt: Any = ...
    n: int = ...
    len: Any = ...
    def __init__(self, catarray: Any) -> None: ...
    def generate_value_label(self, byteorder: Any, encoding: Any): ...

class StataMissingValue(StringMixin):
    MISSING_VALUES: Any = ...
    bases: Any = ...
    float32_base: bytes = ...
    increment: Any = ...
    value: Any = ...
    int_value: Any = ...
    float64_base: bytes = ...
    BASE_MISSING_VALUES: Any = ...
    def __init__(self, value: Any) -> None: ...
    string: Any = ...
    def __unicode__(self): ...
    def __eq__(self, other: Any): ...
    @classmethod
    def get_base_missing_value(cls, dtype: Any): ...

class StataParser:
    DTYPE_MAP: Any = ...
    DTYPE_MAP_XML: Any = ...
    TYPE_MAP: Any = ...
    TYPE_MAP_XML: Any = ...
    VALID_RANGE: Any = ...
    OLD_TYPE_MAPPING: Any = ...
    MISSING_VALUES: Any = ...
    NUMPY_TYPE_MAP: Any = ...
    RESERVED_WORDS: Any = ...
    def __init__(self) -> None: ...

class StataReader(StataParser, BaseIterator):
    __doc__: Any = ...
    col_sizes: Any = ...
    path_or_buf: Any = ...
    def __init__(self, path_or_buf: Any, convert_dates: bool = ..., convert_categoricals: bool = ..., index_col: Optional[Any] = ..., convert_missing: bool = ..., preserve_dtypes: bool = ..., columns: Optional[Any] = ..., order_categoricals: bool = ..., encoding: Optional[Any] = ..., chunksize: Optional[Any] = ...) -> None: ...
    def __enter__(self): ...
    def __exit__(self, exc_type: Any, exc_value: Any, traceback: Any) -> None: ...
    def close(self) -> None: ...
    def data(self, **kwargs: Any): ...
    def __next__(self): ...
    def get_chunk(self, size: Optional[Any] = ...): ...
    def read(self, nrows: Optional[Any] = ..., convert_dates: Optional[Any] = ..., convert_categoricals: Optional[Any] = ..., index_col: Optional[Any] = ..., convert_missing: Optional[Any] = ..., preserve_dtypes: Optional[Any] = ..., columns: Optional[Any] = ..., order_categoricals: Optional[Any] = ...): ...
    def data_label(self): ...
    def variable_labels(self): ...
    def value_labels(self): ...

class StataWriter(StataParser):
    type_converters: Any = ...
    def __init__(self, fname: Any, data: Any, convert_dates: Optional[Any] = ..., write_index: bool = ..., encoding: str = ..., byteorder: Optional[Any] = ..., time_stamp: Optional[Any] = ..., data_label: Optional[Any] = ..., variable_labels: Optional[Any] = ...) -> None: ...
    def write_file(self) -> None: ...

class StataStrLWriter:
    df: Any = ...
    columns: Any = ...
    def __init__(self, df: Any, columns: Any, version: int = ..., byteorder: Optional[Any] = ...) -> None: ...
    def generate_table(self): ...
    def generate_blob(self, gso_table: Any): ...

class StataWriter117(StataWriter):
    def __init__(self, fname: Any, data: Any, convert_dates: Optional[Any] = ..., write_index: bool = ..., encoding: str = ..., byteorder: Optional[Any] = ..., time_stamp: Optional[Any] = ..., data_label: Optional[Any] = ..., variable_labels: Optional[Any] = ..., convert_strl: Optional[Any] = ...) -> None: ...
