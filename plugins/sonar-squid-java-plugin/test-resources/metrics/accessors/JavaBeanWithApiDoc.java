
public class JavaBeanWithAPIDoc {
  
  /**
   * Not to count
   */
  private String testVar;
  
  /**
   * To count
   */
  public String testVar2;
  
  /**
   * This api doc should not be counted not its complexity
   * @return
   */
  public String getTest() {
    return testVar;
  }
  
  /**
   * This api doc should be counted
   * @return
   */
  public String getTest2() {
    return "test";
  }
  
}