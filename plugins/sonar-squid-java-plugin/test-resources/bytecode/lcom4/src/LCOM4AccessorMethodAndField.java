

public class LCOM4AccessorMethodAndField {
  
  public static String field;

  public String getField() {
    return field;
  }
  
  public String doSomethingWithFieldDirect() {
    return field;
  }
  
  public String doSomethingWithFieldIndirect() {
    return getField();
  }
  
}
