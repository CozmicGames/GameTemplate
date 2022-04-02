package engine.graphics.ui.immediate

fun ImmediateUI.label(obj: Any, element: ImmediateUI.Element = getLastElement()) = label(obj.toString(), element)
