

import java.io.Serializable;

public class UnusedPrivateMethod implements Serializable {

  public UnusedPrivateMethod() {
    init();
  }

  private void init() {

  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    //those methods should not be considered as dead code, see Serializable contract
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    //those methods should not be considered as dead code, see Serializable contract
  }
  
  private Object writeReplace() throws java.io.ObjectStreamException{
    //those methods should not be considered as dead code, see Serializable contract
    return null;
  }
  
  private Object readResolve() throws java.io.ObjectStreamException{
    //those methods should not be considered as dead code, see Serializable contract
    return null;
  }

  private int unusedPrivateMethod(java.util.List<java.lang.String> list, int[] measures) {
    return 1;
  }
}
