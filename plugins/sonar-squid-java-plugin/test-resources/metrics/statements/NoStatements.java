public class Car {
  @TestAnnotation
	private int n;
	private String s;

	public Car(int n, String s) {
		this.n = n;
		this.s = s;
		assert true : "not true";
	}

	public int getN() {
		int i =4;
		return n;
	}

	public String getS() {
		return s;
	}
	
	@SuppressWarnings("deprecation")
	public void simpleIf(@TestAnnotation String var){
	  @TestAnnotation
	  int varDecl = -1;
		if( s.equals("toto") ){
			n = 4;
		} else {
			n = 3;
		}
		assert true : "not true";
	}
}