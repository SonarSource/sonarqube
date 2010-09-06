import java.util.Comparator;

class AnonymousInnerClass {
  
  public void doJob(){
    
    Comparator<String> comparator1 = new Comparator<String>() {
      public int compare(String o1, String o2) {
        return 0;
      }
    };
    
    Comparator<String> comparator2 = new Comparator<String>() {
      public int compare(String o1, String o2) {
        return 0;
      }
    };
  }

}
