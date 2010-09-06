
public class UnusedPrivateConstructor {
  
  private String parameter;
  
  private UnusedPrivateConstructor() {
    
  }
  
  private UnusedPrivateConstructor(String parameter) {
    this.parameter = parameter;
  }
  
}
