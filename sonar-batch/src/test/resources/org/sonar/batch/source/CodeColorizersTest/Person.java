/** 
 * Doc
 */
public class Person {
  
  private int first;
  
  @Deprecated
  public void foo(int first, String last, Double middle) {
    this.first = first; // First
  }
}
