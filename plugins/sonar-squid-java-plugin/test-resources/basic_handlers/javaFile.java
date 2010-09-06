/**
 * Javadoc for this Car class
 * @author freddy
 *
 */
public class Car {
	
	private int n;  //C++ inline comment
	private String s;

	public AClass(int n, String s) {
		this.n = n;
		this.s = s;
	}

	/*
	 *C style comment
	 * 
	 */
	public int getN() { 
		/*
		 * Another C style comment
		 */
		return n;
	}

	public String getS() {
		return s;
	}
	
}