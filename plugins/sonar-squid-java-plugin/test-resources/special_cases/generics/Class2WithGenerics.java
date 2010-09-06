
public class Class1WithGenerics  extends Class2WithGenerics {
	public <T> T bar() { return this.<T>foo(); }
	public <T> T foo() { return super.<T>foo(); }
	}
