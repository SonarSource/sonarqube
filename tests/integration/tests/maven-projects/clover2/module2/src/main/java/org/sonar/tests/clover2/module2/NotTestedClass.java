package org.sonar.tests.clover2.module2;

public class NotTestedClass {
  private String hello;
  
  public NotTestedClass(String s){
    this.hello = s;
  }
  
  public String say() {
  	return hello;
  }

  public String hello() {
  	return hello;
  }
}
