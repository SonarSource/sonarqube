public class LCOM4WithWorkers {
  private String field = "toto";
 
  public String getField() {
    return field;
  }
 
  public void doSomething() {
    task1();
    field = "tata";
  }
 
  public void doSomethingElse() {
    task2();
    getField();
  }
 
  public void task1() {
    System.out.println("Hello 1");
    System.out.println("Hello 2");
  }
 
  public void task2() {
    System.out.println("Hello 1");
    System.out.println("Hello 2");
  }
}
