package tests

package implicitConversions

given Conversion[A, B] {
    def apply(a: A): B = ???
}

extension (a: A) def extended_bar(): String = ???

class A {
    implicit def conversion(c: C): D = ???
    implicit def conversion: Conversion[C,D] = ???
    implicit val a: Conversion[C,D] = ???

    extension (c: C) def extended_bar(): String = ???

    class C {
        def bar: String = ???
    }

    class D extends E() {
        def bar2: String = ???

        val string: String = ???

        class Bar()

        type ImplicitType >: String

        extension (e: E) def extended_bar(): String = ???
    }

    class E {
        def inherited: Int = ???
    }
}

class B {
    def foo: Int = ???
}