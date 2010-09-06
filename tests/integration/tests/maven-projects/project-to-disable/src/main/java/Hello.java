public class Hello {

	private Hello() {
		
	}
	
	public void hello() {
		System.out.println("hello" + " world");
	}
	
	protected String getHello() {
		return "hello";
	}
}