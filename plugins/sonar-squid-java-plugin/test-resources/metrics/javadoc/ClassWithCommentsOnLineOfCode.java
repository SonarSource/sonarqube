

public class ClassWithCommentsOnLineOfCode {

    public void run() {
      int i = 5; // comment on line of code, ncloc should not be impacted
      int j = 5; /** test comment 
      tst 2 comment
      **/
      int k = 5; /* another test comment 
      */
    }
    
}
