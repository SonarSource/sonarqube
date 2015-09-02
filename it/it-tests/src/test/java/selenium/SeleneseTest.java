package selenium;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class SeleneseTest {
  private final Selenese suite;

  private Map<String, String> variables;
  private String baseUrl;
  private SeleniumDriver driver;

  public SeleneseTest(Selenese suite) {
    this.suite = suite;
  }

  public void runOn(Orchestrator orchestrator) {
    this.variables = new HashMap<>();
    this.baseUrl = orchestrator.getServer().getUrl();
    this.driver = Browser.FIREFOX.getDriverForThread();

    driver.manage().deleteAllCookies();

    for (File file : suite.getHtmlTests()) {
      System.out.println();
      System.out.println("============ " + file.getName() + " ============");
      Document doc = parse(file);
      for (Element table : doc.getElementsByTag("table")) {
        for (Element tbody : table.getElementsByTag("tbody")) {
          for (Element tr : tbody.getElementsByTag("tr")) {
            String action = tr.child(0).text();
            String param1 = tr.child(1).text();
            String param2 = tr.child(2).text();

            action(action, param1, param2);
          }
        }
      }
    }
  }

  private Document parse(File file) {
    try {
      return Jsoup.parse(file, UTF_8.name());
    } catch (IOException e) {
      throw new RuntimeException("Unable to parse file: " + file, e);
    }
  }

  public SeleneseTest action(String action, String param1, String param2) {
    switch (action) {
      case "open":
        open(param1, param2);
        return this;
      case "type":
        type(param1, param2);
        return this;
      case "select":
        select(param1, param2);
        return this;
      case "clickAndWait":
      case "click":
        click(param1, param2);
        return this;
      case "check":
        check(param1, param2);
        return this;
      case "selectFrame":
        selectFrame(param1, param2);
        return this;
      case "assertElementPresent":
        assertElementPresent(param1, param2);
        return this;
      case "assertElementNotPresent":
        assertElementNotPresent(param1, param2);
        return this;
      case "storeText":
        storeText(param1, param2);
        return this;
      case "storeEval":
        storeEval(param1, param2);
        return this;
      case "assertText":
      case "waitForText":
        assertText(param1, param2);
        return this;
      case "assertNotText":
      case "waitForNotText":
        assertNotText(param1, param2);
        return this;
      case "assertTextPresent":
        assertTextPresent(param1, param2);
      case "assertTextNotPresent":
        assertTextNotPresent(param1, param2);
        return this;
      case "assertLocation":
        assertLocation(param1, param2);
        return this;
      case "waitForElementPresent":
        waitForElementPresent(param1, param2);
        return this;
      case "waitForVisible":
        waitForVisible(param1, param2);
        return this;
      case "assertValue":
      case "waitForValue":
      case "verifyValue":
        assertInputValue(param1, param2);
        return this;
      case "assertConfirmation":
        confirm(param1, param2);
        return this;
      case "setTimeout":
        // Ignore
        return this;
    }

    throw new IllegalArgumentException("Unsupported action: " + action);
  }

  private void goTo(String url) {
    requireNonNull(url, "The url cannot be null");

    URI uri = URI.create(url.replace(" ", "%20"));
    if (!uri.isAbsolute()) {
      url = baseUrl + url;
    }

    System.out.println("goTo " + url);
    driver.get(url);
    System.out.println(" - current url " + driver.getCurrentUrl());
  }

  private void open(String url, String ignored) {
    if (url.startsWith("/sonar/")) {
      goTo(url.substring(6));
    } else {
      goTo(url);
    }
  }

  private LazyDomElement find(String selector) {
    selector = replacePlaceholders(selector);

    if (selector.startsWith("link=")) {
      return find("a").withText(selector.substring(5));
    }

    By by;
    if (selector.startsWith("//")) {
      by = new By.ByXPath(selector);
    } else if (selector.startsWith("xpath=")) {
      by = new By.ByXPath(selector.substring(6));
    } else if (selector.startsWith("id=")) {
      by = new By.ById(selector.substring(3));
    } else {
      by = new ByCssSelectorOrByNameOrById(cleanUp(selector));
    }

    return new LazyDomElement(driver, by);
  }

  private void click(String selector, String ignored) {
    find(selector).click();
  }

  private void check(String selector, String ignored) {
    find(selector).check();
  }

  private void selectFrame(final String id, String ignored) {
    if ("relative=parent".equals(id)) {
      //driver().switchTo().parentFrame();
    } else {
      System.out.println(" - selectFrame(" + id + ")");

      Retry._5_SECONDS.execute(new Runnable() {
        @Override
        public void run() {
          driver.switchTo().frame(id);
        }
      });
    }
  }

  private String cleanUp(String selector) {
    if (selector.startsWith("name=")) {
      return selector.substring(5);
    }
    if (selector.startsWith("css=")) {
      return selector.substring(4);
    }
    if (selector.startsWith("id=")) {
      return "#" + selector.substring(3);
    }
    if (selector.startsWith("class=")) {
      return "." + selector.substring(6);
    }
    return selector;
  }

  private void type(String selector, String text) {
    find(selector).fill(replacePlaceholders(text));
  }

  private void select(String selector, String text) {
    if (text.startsWith("label=")) {
      find(selector).select(text.substring(6));
    } else {
      find(selector).select(text);
    }
  }

  private void assertElementPresent(String selector, String ignored) {
    find(selector).should().beDisplayed();
  }

  private void assertElementNotPresent(String selector, String ignored) {
    find(selector).should().not().beDisplayed();
  }

  private void storeText(String selector, String name) {
    find(selector).execute(new ExtractVariable(name));
  }

  private void storeEval(String expression, String name) {
    String value = driver.executeScript("return " + expression).toString();
    variables.put(name, value);
  }

  private class ExtractVariable implements Consumer<WebElement> {
    private final String name;

    ExtractVariable(String name) {
      this.name = name;
    }

    @Override
    public void accept(WebElement webElement) {
      variables.put(name, webElement.getText());
    }

    public String toString() {
      return "read value into " + name;
    }
  }

  private void assertText(String selector, String pattern) {
    pattern = replacePlaceholders(pattern);

    if (pattern.startsWith("exact:")) {
      String expectedText = pattern.substring(6);
      find(selector).withText(expectedText).should().exist();
      return;
    }

    if (pattern.startsWith("regexp:")) {
      String expectedRegEx = pattern.replaceFirst("regexp:", ".*") + ".*";
      find(selector).should().match(Pattern.compile(expectedRegEx, Pattern.DOTALL));
      return;
    }

    find(selector).should().match(glob(pattern));
  }

  private void assertNotText(String selector, String pattern) {
    pattern = replacePlaceholders(pattern);

    if (pattern.startsWith("exact:")) {
      String expectedText = pattern.substring(6);
      find(selector).withText(expectedText).should().not().exist();
      return;
    }

    if (pattern.startsWith("regexp:")) {
      String expectedRegEx = pattern.replaceFirst("regexp:", ".*") + ".*";
      find(selector).should().not().match(Pattern.compile(expectedRegEx, Pattern.DOTALL));
      return;
    }

    find(selector).should().not().match(glob(pattern));
  }

  private Pattern glob(String pattern) {
    String expectedGlob = pattern.replaceFirst("glob:", "");
    expectedGlob = expectedGlob.replaceAll("([\\]\\[\\\\{\\}$\\(\\)\\|\\^\\+.])", "\\\\$1");
    expectedGlob = expectedGlob.replaceAll("\\*", ".*");
    expectedGlob = expectedGlob.replaceAll("\\?", ".");
    return Pattern.compile(expectedGlob, Pattern.DOTALL);
  }

  private void assertTextPresent(String text, String ignored) {
    find("html").should().contain(text);
  }

  private void assertTextNotPresent(String text, String ignored) {
    find("html").should().not().contain(text);
  }

  private void waitForElementPresent(String selector, String ignored) {
    find(selector).should().exist();
  }

  private void waitForVisible(String selector, String ignored) {
    find(selector).should().beDisplayed();
  }

  private void assertInputValue(String selector, String text) {
    find(selector).should().contain(text);
  }

  private void confirm(final String message, String ignored) {
    System.out.println(" - confirm(" + message + ")");

    Retry._5_SECONDS.execute(new Runnable() {
      @Override
      public void run() {
        Alert alert = driver.switchTo().alert();
        if (alert.getText().contains(message)) {
          alert.accept();
        }
      }
    });
  }

  private void assertLocation(String urlPattern, String ignored) {
    assertThat(driver.getCurrentUrl()).matches(glob(urlPattern));
  }

  private String replacePlaceholders(String text) {
    for (Map.Entry<String, String> entry : variables.entrySet()) {
      text = text.replace("${" + entry.getKey() + "}", entry.getValue());
    }
    return text;
  }
}
