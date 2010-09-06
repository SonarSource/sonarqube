

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UnusedProtectedMethod extends Job {
  
  public UnusedProtectedMethod() {
    init();
  }
  
  private void init() {
    
  }
  
  protected List<String> unusedProtectedMethod(Locale locale, int[] measures) {
    return new ArrayList<String>();
  }
  
  @Override
  public void doJob(){
    
  }
  
  @Override
  protected void doBeforeJob(){
    //this method should not be considered as dead code as it implements an protected and expected behavior
  }
  
  protected Object writeReplace() throws java.io.ObjectStreamException{
    //this method should not be considered as dead code, see Serializable contract
    return null;
  }
  
  protected Object readResolve() throws java.io.ObjectStreamException{
    //this method should not be considered as dead code, see Serializable contract
    return null;
  }
}
