package properties;

public class ResourceTouchedProperty implements Runnable {
  
  private String unusedField;
  private String usedField;
  
  public void notInheritedMethod(){
  }
  
  public boolean equals(Object object){
    return false;
  }
  
  public void run(){
    doPrivateJob();
  }
  
  private void doPrivateJob(){
    usedField = "value";
  }
}
