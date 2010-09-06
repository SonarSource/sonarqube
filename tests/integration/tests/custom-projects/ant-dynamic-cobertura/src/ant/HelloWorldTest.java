package ant;

public class HelloWorldTest extends junit.framework.TestCase {

    public void testClone() {
	  HelloWorld hello = new HelloWorld();
	  assertEquals(hello, hello.clone());
    }
    
    public void testWillAlwaysFail() {
        fail("An error message");
    }
    
}