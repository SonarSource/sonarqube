/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package util.selenium;

import com.sonar.orchestrator.Orchestrator;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.assertj.core.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.sonarqube.qa.util.SelenideConfig;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.regex.Pattern.DOTALL;
import static org.assertj.core.api.Assertions.assertThat;
import static util.selenium.Retry._30_SECONDS;

class SeleneseRunner {

  private Map<String, String> variables;
  private String baseUrl;
  private WebDriver driver;

  void runOn(Selenese selenese, Orchestrator orchestrator) {
    this.variables = new HashMap<>();
    this.baseUrl = orchestrator.getServer().getUrl();
    this.driver = SelenideConfig.configure(orchestrator);

    driver.manage().deleteAllCookies();

    for (File file : selenese.getHtmlTests()) {
      System.out.println();
      System.out.println("============ " + file.getName() + " ============");
      Document doc = parse(file);
      for (Element table : doc.getElementsByTag("table")) {
        for (Element tbody : table.getElementsByTag("tbody")) {
          for (Element tr : tbody.getElementsByTag("tr")) {
            String action = tr.child(0).text();
            String param1 = tr.child(1).text();
            String param2 = tr.child(2).text();

            try {
              action(action, param1, param2);
            } catch (AssertionError e) {
              analyzeLog(driver);
              throw e;
            }
          }
        }
      }
    }
  }

  private static void analyzeLog(WebDriver driver) {
    LogEntries logEntries = driver.manage().logs().get(LogType.BROWSER);
    for (LogEntry entry : logEntries) {
      System.out.println(new Date(entry.getTimestamp()) + " " + entry.getLevel() + " " + entry.getMessage());
    }
  }

  private static Document parse(File file) {
    try {
      return Jsoup.parse(file, UTF_8.name());
    } catch (IOException e) {
      throw new RuntimeException("Unable to parse file: " + file, e);
    }
  }

  public SeleneseRunner action(String action, String param1, String param2) {
    switch (action) {
      case "open":
        open(param1);
        return this;
      case "type":
        type(param1, param2);
        return this;
      case "keyPressAndWait":
        keyPressAndWait(param1, param2);
        return this;
      case "select":
        select(param1, param2);
        return this;
      case "clickAndWait":
      case "click":
        click(param1);
        return this;
      case "check":
        check(param1);
        return this;
      case "selectFrame":
        selectFrame(param1);
        return this;
      case "assertElementPresent":
        assertElementPresent(param1);
        return this;
      case "assertElementNotPresent":
        assertElementNotPresent(param1);
        return this;
      case "storeText":
        storeText(param1, param2);
        return this;
      case "storeEval":
        storeEval(param1, param2);
        return this;
      case "store":
        store(param1, param2);
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
        assertTextPresent(param1);
        return this;
      case "assertTextNotPresent":
        assertTextNotPresent(param1);
        return this;
      case "assertLocation":
        assertLocation(param1);
        return this;
      case "verifyHtmlSource":
        verifyHtmlSource(param1);
        return this;
      case "waitForElementPresent":
        waitForElementPresent(param1, param2);
        return this;
      case "waitForElementNotPresent":
        waitForElementNotPresent(param1, param2);
        return this;
      case "waitForVisible":
        waitForVisible(param1);
        return this;
      case "waitForXpathCount":
        waitForXpathCount(param1, Integer.parseInt(param2));
        return this;
      case "assertValue":
      case "waitForValue":
      case "verifyValue":
        assertInputValue(param1, param2);
        return this;
      case "assertConfirmation":
        confirm(param1);
        return this;
      case "setTimeout":
      case "pause":
        // Ignore
        return this;
    }

    throw new IllegalArgumentException("Unsupported action: " + action);
  }

  private void open(String url) {
    if (url.startsWith("/sonar/")) {
      goTo(url.substring(6));
    } else {
      goTo(url);
    }
  }

  private void goTo(String url) {
    requireNonNull(url, "The url cannot be null");

    url = replacePlaceholders(url);

    URI uri = URI.create(url.replace(" ", "%20").replace("|", "%7C"));
    if (!uri.isAbsolute()) {
      url = baseUrl + url;
    }

    System.out.println("goTo " + url);
    driver.get(url);
    System.out.println(" - current url " + driver.getCurrentUrl());
  }

  private LazyDomElement find(String selector) {
    selector = replacePlaceholders(selector);

    if (selector.startsWith("link=") || selector.startsWith("Link=")) {
      return find("a").withText(selector.substring(5));
    }

    By by;
    if (selector.startsWith("//")) {
      by = new By.ByXPath(selector);
    } else if (selector.startsWith("xpath=")) {
      by = new By.ByXPath(selector.substring(6));
    } else if (selector.startsWith("id=")) {
      by = new By.ById(selector.substring(3));
    } else if (selector.startsWith("name=")) {
      by = new By.ByName(selector.substring(5));
    } else if (selector.startsWith("css=")) {
      by = new By.ByCssSelector(selector.substring(4));
    } else if (selector.startsWith("class=")) {
      by = new By.ByCssSelector("." + selector.substring(6));
    } else {
      by = new ByCssSelectorOrByNameOrById(selector);
    }

    return new LazyDomElement(driver, by);
  }

  private void click(String selector) {
    find(selector).click();
  }

  private void check(String selector) {
    find(selector).check();
  }

  private void selectFrame(final String id) {
    if ("relative=parent".equals(id)) {
      return;
    }

    System.out.println(" - selectFrame(" + id + ")");
    _30_SECONDS.execute(new Runnable() {
      @Override
      public void run() {
        driver.switchTo().frame(id);
      }
    });
  }

  private void type(String selector, String text) {
    find(selector).fill(replacePlaceholders(text));
  }

  private void keyPressAndWait(String selector, String key) {
    if (!key.equals("\\13")) {
      throw new IllegalArgumentException("Invalid key: " + key);
    }
    find(selector).pressEnter();
  }

  private void select(String selector, String text) {
    if (text.startsWith("label=")) {
      find(selector).select(text.substring(6));
    } else {
      find(selector).select(text);
    }
  }

  private void assertElementPresent(String selector) {
    find(selector).should().beDisplayed();
  }

  private void assertElementNotPresent(String selector) {
    find(selector).should().not().beDisplayed();
  }

  private void storeText(String selector, String name) {
    find(selector).execute(new ExtractVariable(name));
  }

  private void storeEval(final String expression, final String name) {
    // Retry until it's not null and doesn't fail
    _30_SECONDS.execute(new Runnable() {
      @Override
      public void run() {
        Object result = ((JavascriptExecutor) driver).executeScript("return " + expression);
        if (result == null) {
          throw new NotFoundException(expression);
        }
        String value = result.toString();
        variables.put(name, value);
      }
    });
  }

  private void store(String expression, String name) {
    if (expression.startsWith("javascript{") && expression.endsWith("}")) {
      storeEval(expression.substring(11, expression.length() - 1), name);
    } else {
      throw new IllegalArgumentException("Invalid store expression: " + expression);
    }
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
      find(selector).should().match(regex(pattern));
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
      find(selector).should().not().match(regex(pattern));
      return;
    }

    find(selector).should().not().match(glob(pattern));
  }

  private static Pattern glob(String pattern) {
    String regexp = pattern.replaceFirst("glob:", "");
    regexp = regexp.replaceAll("([\\]\\[\\\\{\\}$\\(\\)\\|\\^\\+.])", "\\\\$1");
    regexp = regexp.replaceAll("\\*", ".*");
    regexp = regexp.replaceAll("\\?", ".");
    return Pattern.compile(regexp, DOTALL | Pattern.CASE_INSENSITIVE);
  }

  private static Pattern regex(String pattern) {
    String regexp = pattern.replaceFirst("regexp:", ".*") + ".*";
    return Pattern.compile(regexp, DOTALL | Pattern.CASE_INSENSITIVE);
  }

  private void assertTextPresent(String text) {
    find("body").should().contain(text);
  }

  private void assertTextNotPresent(String text) {
    find("body").should().not().contain(text);
  }

  private void waitForElementPresent(String selector, String text) {
    if (Strings.isNullOrEmpty(text)) {
      find(selector).should().exist();
    } else {
      find(selector).withText(text).should().exist();
    }
  }

  private void waitForElementNotPresent(String selector, String text) {
    if (Strings.isNullOrEmpty(text)) {
      find(selector).should().not().exist();
    } else {
      find(selector).withText(text).should().not().exist();
    }
  }

  private void waitForVisible(String selector) {
    find(selector).should().beDisplayed();
  }

  private void assertInputValue(String selector, String text) {
    find(selector).should().contain(text);
  }

  private void waitForXpathCount(String selector, int expectedCount) {
    assertThat(find(selector).stream().size()).isEqualTo(expectedCount);
  }

  private void confirm(final String message) {
    System.out.println(" - confirm(" + message + ")");

    _30_SECONDS.execute(new Runnable() {
      @Override
      public void run() {
        driver.switchTo().alert().accept();
      }
    });
  }

  private void assertLocation(String urlPattern) {
    assertThat(driver.getCurrentUrl()).matches(glob(urlPattern));
  }

  private void verifyHtmlSource(String expect) {
    assertThat(driver.getPageSource()).matches(glob(expect));
  }

  private String replacePlaceholders(String text) {
    for (Map.Entry<String, String> entry : variables.entrySet()) {
      text = text.replace("${" + entry.getKey() + "}", entry.getValue());
    }
    return text;
  }
}
