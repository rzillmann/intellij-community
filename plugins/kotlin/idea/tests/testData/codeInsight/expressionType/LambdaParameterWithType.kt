package test

class Foo

fun test(list: List<Foo>) {
    list.forEach { <caret>foo: Foo -> }
}

// K1_TYPE: { foo: Foo -> } -> <html>(Foo) -&gt; Unit</html>

// K2_TYPE: { foo: Foo -> } -> (Foo) -&gt; Unit