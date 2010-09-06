import java.util.ArrayList;

public class ComplexBranchesTest {
	private int n;

	public ComplexBranchesTest(int n) {
		this.n = n;
	}

	public int getN() {
		if (n==1) {
		  return -1;
		} else if (n==2) {
		  return -2;
		} else if (n==3) {
		  throw new RuntimeException("Error");
		}
		return -1;
	}

}