// WITH_STDLIB
// INTENTION_TEXT: "Remove return@label"

fun foo() {
    listOf(1,2,3).find label@{
        <caret>return@label true
    }
}