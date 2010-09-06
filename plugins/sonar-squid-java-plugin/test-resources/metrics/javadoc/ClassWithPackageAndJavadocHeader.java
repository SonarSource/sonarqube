/**
 * File header as javadoc
 * should be detected as a header
 * 
 */
package foo;

/**
 * This is a class comment
 */
public class ClassWithPackageAndJavadocHeader {

	public static void doJob1() {
		int i = 3;
		if (i == 4) {
			i++;
		}
	}
	
	/**
	 * 
	 * qsdfqsdf
	 */
	public static void doJob2() {
		int i = 3;
		if (i == 4) {
			if (i == 3) {
				i--;
				//mlkqjdsf
			}
		}
	}
	public class Toto{
		public static void doJob2() {
			int i = 3;
			if (i == 4) {
				if (i == 3) {
					i--;
				}
			}
		}
		
	}
	
	
	
	
	

}
