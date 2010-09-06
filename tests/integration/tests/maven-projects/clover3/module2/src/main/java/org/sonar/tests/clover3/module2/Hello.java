package org.sonar.tests.clover3.module2;

public class Hello {
  private String hello;
  
  public Hello(String s){
    this.hello = s;
  }
  
  public String say() {
  	return hello;
  }
}
