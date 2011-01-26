public class FailTest extends junit.framework.TestCase {

    public void testNothing() {
    }
    
    public void testWillAlwaysFail() {
        fail("An error message");
    }
    
}