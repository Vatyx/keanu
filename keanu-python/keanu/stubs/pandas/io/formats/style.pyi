# Stubs for pandas.io.formats.style (Python 3.6)
#
# NOTE: This dynamically typed stub was automatically generated by stubgen.

from typing import Any, Optional

has_mpl: bool
no_mpl_message: str

class Styler:
    loader: Any = ...
    env: Any = ...
    template: Any = ...
    ctx: Any = ...
    data: Any = ...
    index: Any = ...
    columns: Any = ...
    uuid: Any = ...
    table_styles: Any = ...
    caption: Any = ...
    precision: Any = ...
    table_attributes: Any = ...
    hidden_index: bool = ...
    hidden_columns: Any = ...
    cell_ids: Any = ...
    def __init__(self, data: Any, precision: Optional[Any] = ..., table_styles: Optional[Any] = ..., uuid: Optional[Any] = ..., caption: Optional[Any] = ..., table_attributes: Optional[Any] = ..., cell_ids: bool = ...) -> None: ...
    def to_excel(self, excel_writer: Any, sheet_name: str = ..., na_rep: str = ..., float_format: Optional[Any] = ..., columns: Optional[Any] = ..., header: bool = ..., index: bool = ..., index_label: Optional[Any] = ..., startrow: int = ..., startcol: int = ..., engine: Optional[Any] = ..., merge_cells: bool = ..., encoding: Optional[Any] = ..., inf_rep: str = ..., verbose: bool = ..., freeze_panes: Optional[Any] = ...) -> None: ...
    def format(self, formatter: Any, subset: Optional[Any] = ...): ...
    def render(self, **kwargs: Any): ...
    def __copy__(self): ...
    def __deepcopy__(self, memo: Any): ...
    def clear(self) -> None: ...
    def apply(self, func: Any, axis: int = ..., subset: Optional[Any] = ..., **kwargs: Any): ...
    def applymap(self, func: Any, subset: Optional[Any] = ..., **kwargs: Any): ...
    def where(self, cond: Any, value: Any, other: Optional[Any] = ..., subset: Optional[Any] = ..., **kwargs: Any): ...
    def set_precision(self, precision: Any): ...
    def set_table_attributes(self, attributes: Any): ...
    def export(self): ...
    def use(self, styles: Any): ...
    def set_uuid(self, uuid: Any): ...
    def set_caption(self, caption: Any): ...
    def set_table_styles(self, table_styles: Any): ...
    def hide_index(self): ...
    def hide_columns(self, subset: Any): ...
    def highlight_null(self, null_color: str = ...): ...
    def background_gradient(self, cmap: str = ..., low: int = ..., high: int = ..., axis: int = ..., subset: Optional[Any] = ..., text_color_threshold: float = ...): ...
    def set_properties(self, subset: Optional[Any] = ..., **kwargs: Any): ...
    def bar(self, subset: Optional[Any] = ..., axis: int = ..., color: str = ..., width: int = ..., align: str = ..., vmin: Optional[Any] = ..., vmax: Optional[Any] = ...): ...
    def highlight_max(self, subset: Optional[Any] = ..., color: str = ..., axis: int = ...): ...
    def highlight_min(self, subset: Optional[Any] = ..., color: str = ..., axis: int = ...): ...
    @classmethod
    def from_custom_template(cls, searchpath: Any, name: Any): ...