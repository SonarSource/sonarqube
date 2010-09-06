
public class CallToDeprecatedMethod {
  
  public CallToDeprecatedMethod() {
    String string = new String("my string");
    string.getBytes(1, 1, new byte[3], 7); //call to deprecated method
  }
}
