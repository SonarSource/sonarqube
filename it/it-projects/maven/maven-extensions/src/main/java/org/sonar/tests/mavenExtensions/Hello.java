package org.sonar.tests.mavenExtensions;

public class Hello {
  private String hello;
  
  public Hello(String s){
    this.hello = s;
  }
  
  public String say() {
  	return hello;
  }
}
