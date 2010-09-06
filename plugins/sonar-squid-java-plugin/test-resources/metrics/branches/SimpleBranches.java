import java.util.ArrayList;

public class Car {
	private int n;
	private String s;

	public AClass(int n, String s) {
		this.n = n;
		this.s = s;
	}

	public int getN() {
		return n;
	}

	public String getS() {
		return s;
	}

	private void simpleIf() {
		if (s.equals("road") && n != 1) {
			n++;
		}
	}
	
	private void ifWithOr() {
		if (s.equals("road") || n != 1) {
			n++;
		}
	}

	private void simpleSwitch() {
		switch (n) {
		case 1:
			break;
		case 2:
			break;
		default:
			break;
		}
	}
	
	private void simpleFor() {
		for (int i = 0; i < 4; i++) {
			int j = 4;
		}
		for (String toto : new ArrayList<String>()) {
			int k = 0;
		}
	}

}