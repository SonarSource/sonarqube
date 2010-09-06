/**
 * Javadoc for this class
 * @author freddy
 *
 */
public class JavaClassWithGWTNativeCode {
	
  //Java method declaration...
  native String flipName(String name) /*-{

    // ...implemented with JavaScript
    var re = /(\w+)\s(\w+)/;
    return name.replace(re, '$2, $1');

  }-*/;
	
}