public class UnicodeEscape {

  public void foo() {
    char a = '\u005Cr';
    char b = '\uuuu005Cr';
    char c = '\u005c\u005c';
  }

}
