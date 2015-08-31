package selenium;

import org.openqa.selenium.WebElement;

class WebElementHelper {
  WebElementHelper() {
    // Static class
  }

  public static String text(WebElement element) {
    String text = element.getText();
    if (!"".equals(text)) {
      return nullToEmpty(text);
    }

    return nullToEmpty(element.getAttribute("value"));
  }

  private static String nullToEmpty(String text) {
    return (text == null) ? "" : text;
  }
}
