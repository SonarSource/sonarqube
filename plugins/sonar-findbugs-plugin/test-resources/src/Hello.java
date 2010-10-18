class Hello {

  static String name;
  
  public void methodWithViolations(String n) {
    name = n;
  }
  
  @Override
  public boolean equals(Object obj) {
    return false;
  }
}