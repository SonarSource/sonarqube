

public abstract class LCOM4Exclusions implements Runnable {
  
  public static String field1;
  public static String field2;

  public LCOM4Exclusions(){}
  
  public static void doWork(){}
  
  public void emptyMethod(){}
  
  public abstract void doAbstractWork();
  
  public void run(){
    run2();
  }
  
  public void run2(){
    run();
  }
  
  public boolean equals(Object object){
    return (field1 == null) ? false : field1.equals(field2);
  }
}
