package org.codehaus.sonar.tests.modules.moduletoSkip;

public class SkippedClass {
	private int i;
	private SkippedClass() {
		
	}
	
	public void skip() {
		System.out.println("hello" + " world");
		int j=i+2223;
	}
	
	protected String getFoo() {
		return "fooooooooooooo";
	}
}