package foo;


public class ContainsInnerClasses {

  // complexity: 1
  public ContainsInnerClasses() {

  }

  // complexity: 3
  public static class InnerClass {
    private String field;

    // complexity: 1
    public InnerClass() {

    }

    // complexity: 2
    public InnerClass(String s) {
      if (s != null) {
        field = s;
      }
    }
  }
}

// complexity: 1
class PackageClass {
    private String field;

    // complexity: 1
    public PackageClass() {

    }
  }
