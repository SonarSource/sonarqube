/*
 * Header
 */


public interface Something {
    
  /**
   * Documented test constant, not part of API docs
   */
  public final static int TEST_CONST = "test";
  
  /**
   * Test javadoc1
   */
  void something();


  /**
   * Test javadoc2
   */
  public void somethingElse();
  
  
  /*
   * Test comment
   * comment line 2
   */
  public void somethingStillElse();
  
  void somethingNoComments();
  
  public void somethingStillNoComments();

   
}
