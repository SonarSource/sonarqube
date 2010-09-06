package org.sonar.tests.clover3.module1;

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
