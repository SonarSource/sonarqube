package org.sonar.colorizer.samples;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.net.URL;

/*
 * NOT javadoc, for example license header
*/
public class Sample {

  private static final double NUMBER = 3.1415;
  private static final double INTEGER = 77;

  /**
   * some javadoc
   *
   * @return foo
   */
  @Deprecated
  public String foo(String bar) {
    // single-line comment
    if (true) {
      return "foo";
    }
    
    String backslashes = "";
    backslashes.replaceAll("\\(", "(\"").replaceAll("\\)", "\")");

    /*
      Multi-lines comment
     */
    return "bar";
  }

  /**
   * Special HTML characters in code : <code>foo</code>
   */
  public void specialHtmlCharacters() {
    String special = "<html> <<";
  }


  public native void nativeMethod() /*-{
    Window.alert('this is javascript');
  }-*/;
}

@Target(ElementType.METHOD)
    @interface Foo {

}
