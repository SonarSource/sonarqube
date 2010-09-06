public class ClassWithStaticMethods {

	public static void doJob1() {
		int i = 3;
		if (i == 4) {
			i++;
		}
	}

	public static void doJob2() {
		int i = 3;
		if (i == 4) {
			if (i == 3) {
				i--;
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
