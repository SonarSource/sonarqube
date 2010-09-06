package java.bean.test;

public class PureJavaBean {
  
  private String testVar;
  
  public String testVar2;
  
  String testVar3;
  
  private boolean booleanTest;
  
  // OK
  public String getTest() {
    return testVar;
  }
  
  // OK
  public String getTest2() {
    return testVar;
  }

  // NOT OK
  public String getTest3() {
    testVar = "test";
    return testVar;
  }
  
  // NOT OK
  public String getTest4() {
    return testVar + "concat";
  }
  
  // NOT OK
  public void getTest5() {
    // do something;
  }
  
  // NOT OK
  public String getTest6() {
    return "test";
  }
  
  // NOT OK testVar2 is public
  public String getTest7() {
    return testVar2;
  }
  
  // OK
  public String getTest8() {
    // some comments
    return testVar;
  }

  // OK
  public void setTest(String test) {
    this.testVar = test;
  }
  
  // NOT OK
  public void setTest(String test, String test2) {
    this.testVar = test;
  }
  
  // NOT OK
  public void setTest() {
    this.testVar = test;
  }
  
  // NOT OK
  public void setTest2(String test) {
    this.testVar = "";
  }
  
  // NOT OK
  public void setTest3(String test) {
    this.testVar += test;
  }
  
  // OK
  public void setTest4(String test) {
    // do something
    this.testVar = test;
  }
  
  // NOT OK
  public boolean isTest() {
    return true;
  }
  
  // OK
  public boolean isTest2() {
    return booleanTest;
  }
}
