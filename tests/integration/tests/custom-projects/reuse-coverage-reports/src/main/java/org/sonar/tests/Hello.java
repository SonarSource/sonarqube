package org.sonar.tests;

public class Hello {
  private String hello;
  
  public Hello(String s){
    this.hello = s;
  }
  
  public String say() {
  	return hello;
  }
  
  protected int notCalled() {
    int i=0;
    i++;
    return i+3;	
  }
}
