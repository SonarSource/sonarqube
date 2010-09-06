public class FirstClass {

  private FirstClass() {
    int unused= new Integer(0);
    if (true) {
      unused += new Integer(12345);
    }
  }
}
