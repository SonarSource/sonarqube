/*
 * Header
 */


import nothing;
//single comment 1
/**
 * Javadoc 1
 * blablab
 * blabla
 */
public class Something {
  
    public int var = 0;
    
    /**
     * Documented var
     */
    public int var2 = 0;
    
    /**
     * Non api documented var
     */
    private int var3 = 0;
  
    /**
     * Constructor API doc
     * should be included in api doc counter
     */
    public Something() {
        super();
        /**
         * Inner constructor javadoc
         * Should not be counted as API doc
         */
    }

    public Something(Strng test) {
        super();
    }

    /**
     * Javadoc 2
     */
    public void sleep();

    /*
     * non javadoc
     */
    public void run() {
      /**
       * Inner javadoc
       * 
       * Should not be counted as API doc
       */
        int i = 5;
    }
    
    /**
     * Javadoc
     */
    private void test() {
    }
}
// ncss = 8
