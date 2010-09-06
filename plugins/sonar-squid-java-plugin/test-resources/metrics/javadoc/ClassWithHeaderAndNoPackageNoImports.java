/*
 * Header
 * 
 * but no package and no import, should be detected as a license header
 */
public class Something {
    public Something() {
        super();
    }

    /**
     * Javadoc 2
     */
    public void sleep(){}

    /*
     * non javadoc
     */
    public void run() {
        int i = 5;
    }
}
// ncss = 8
