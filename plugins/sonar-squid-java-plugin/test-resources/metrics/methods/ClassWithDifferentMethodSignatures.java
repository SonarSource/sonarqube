public abstract class ClassWithDifferentMethodSignatures {
  
  public ClassWithDifferentMethodSignatures(){}
  
  public ClassWithDifferentMethodSignatures(List<String> toto){}

	public static void method();
	
	public static String method(int a);
	
	public static double[] method(int a);
	
	 public static void method(String a);
	 
   public static void method(String[][] a);
	 
   public static void method(java.util.ArrayList a);
   
   public void unusedPrivateMethod(java.util.List<java.lang.String> list, int[] measures);
   
   public static void method(Map.Entry a);
	
	public static Squid[] method(int a, double[] b);
	
	 public static java.Squid[] method(Map<Integer, String[]> toto);
}
