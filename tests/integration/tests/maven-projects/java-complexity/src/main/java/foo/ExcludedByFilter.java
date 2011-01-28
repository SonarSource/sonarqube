package foo;

// this class is excluded by the resource filter defined in sonar-it-reference-plugin
public class ExcludedByFilter {

  public void say() {
    int i=0;
    if(i>5) {
      System.out.println("say something");
    }
  }

  public void cry() {
    int i=0;
    if(i<5) {
      System.out.println("cry");
    }
  }
}
