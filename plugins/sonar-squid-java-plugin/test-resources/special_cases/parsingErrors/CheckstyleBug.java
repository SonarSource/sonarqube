import java.util.List;

interface Foo {
  public <T> List<T> bar(T foobar);
}

public class CheckstyleBug {

  public void foo() {
    Foo foo = new Foo() {
      public <T> List<T> bar(T foobar) {
        return null;
      }
    };
  }

}
