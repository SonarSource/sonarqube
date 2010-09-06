package org.sonar.tests;

/**
 * JAVADOC
 * .........
 */
public class ClassToExclude {
  String test = "foo";
  String test2 = "bar";
  String test3 = "toto";

  public ClassToExclude(){
    String t;
    if (true)
      t = "sonar";
  }

  public static final String method1(String unused) {
    if (true) {
      return "foo";
    }
    if (false) {
      return "foooooo";
    }
    String loooooooooooooooooooooooooooooooooooooooooooooongVar = "wantsViolations";
    return loooooooooooooooooooooooooooooooooooooooooooooongVar;
  }

  protected void duplicatedMethod(int i) {
    // commmmmmmments
    // foo..............
    i++;
    int j=10;
    if (i==0) {
      i=j + 10;
    }
    if (i==0) {
      i=j + 10;
    }
    if (i==0) {
      i=j + 10;
    }
    if (i==0) {
      i=j + 10;
    }
    if (i==0) {
      i=j + 10;
    }
    if (i==0) {
      i=j + 10;
    }
    if (i==0) {
      i=j + 10;
    }
    if (i==0) {
      i=j + 10;
    }
    if (i==0) {
      i=j + 10;
    }
    if (i==0) {
      i=j + 10;
    }
    System.out.println("i=" + i);
  }
}
