package innerclass

class InnerClassExample
{
    def show() {
        println 'Hello World'
        new ExampleInnerClass().show()
    }

    class ExampleInnerClass {
        def show() { println "Hello Inner"}
    }

}
