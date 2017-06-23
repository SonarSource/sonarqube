package foo;

/**
 * Simple class
 */
public class Simplest {

    public static int add(int a, int b) {
    	// introduce a variable that is not needed - just to get a violation
    	int result = a + b;

    	System.out.println("");

        return result;
    }

}
