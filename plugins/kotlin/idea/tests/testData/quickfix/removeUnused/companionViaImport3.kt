// "Safe delete 'NamedObject'" "true"
import TestClass.NamedObject

class TestClass{
    private object NamedObject<caret> {
        const val CONST = "abc"
    }
}
// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.inspections.SafeDeleteFix
// IGNORE_K2