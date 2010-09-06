package org.sonar.samples;

/**
 * 
 * THE JAVADOC HEADER
 *
 */
public class ClassWithComments {

    public ClassWithComments(int i) {
	    // single comment
		int j = i++;
	}
	
	private String myMethod() {
		/*
		comment
		on
		many
		lines		
		*/
		int toto=34; // comment at end of line
		return "hello";
	}
}
