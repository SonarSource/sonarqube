class Hello {

  public boolean methodWithViolations() {
    String foo = "foo";
    String bar = "bar";
    if (true) {
      ;;
    }
    return foo==bar;
  }
}