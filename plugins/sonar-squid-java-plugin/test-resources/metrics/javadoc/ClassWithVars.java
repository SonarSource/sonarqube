/*
 * Header
 */


import nothing;
//single comment 1
/**
 * Javadoc 1
 */
public class ClassWithVars {
  
    /**
     * Api doc for this public var
     */
    public int publicVar = 0;
    
    /**
     * Not Api doc for this public const, we exclude public final static var to avoid too much noise on pure Constants Classes
     */
    public final static int PUBLIC_CONST = 0;
    
    /**
     * Api doc for private var should not be counted as apidoc, but non apidoc
     */
    private int pirvateVar = 0;
  
    
    public Something() {
        super();
        /**
         * Inner constructor javadoc
         * Should not be counted as API doc
         */
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
