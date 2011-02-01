package foo;

// class complexity: 5
public class ContainsInnerClasses {

  // method complexity: 1
  public ContainsInnerClasses() {

  }

  // class complexity: 3
  public static class InnerClass {
    private String field;

    // method complexity: 1
    public InnerClass() {

    }

    // method complexity: 2
    public InnerClass(String s) {
      if (s != null) {
        field = s;
      }
    }
  }
}

// class complexity: 1
class PackageClass {
    private String field;

    // method complexity: 1
    public PackageClass() {

    }
  }
