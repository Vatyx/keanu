# Stubs for pandas.io.sas.sas7bdat (Python 3.6)
#
# NOTE: This dynamically typed stub was automatically generated by stubgen.

from pandas.io.common import BaseIterator
from typing import Any, Optional

class _subheader_pointer: ...
class _column: ...

class SAS7BDATReader(BaseIterator):
    index: Any = ...
    convert_dates: Any = ...
    blank_missing: Any = ...
    chunksize: Any = ...
    encoding: Any = ...
    convert_text: Any = ...
    convert_header_text: Any = ...
    default_encoding: str = ...
    compression: str = ...
    column_names_strings: Any = ...
    column_names: Any = ...
    column_formats: Any = ...
    columns: Any = ...
    handle: Any = ...
    def __init__(self, path_or_buf: Any, index: Optional[Any] = ..., convert_dates: bool = ..., blank_missing: bool = ..., chunksize: Optional[Any] = ..., encoding: Optional[Any] = ..., convert_text: bool = ..., convert_header_text: bool = ...) -> None: ...
    def column_data_lengths(self): ...
    def column_data_offsets(self): ...
    def column_types(self): ...
    def close(self) -> None: ...
    def __next__(self): ...
    def read(self, nrows: Optional[Any] = ...): ...
