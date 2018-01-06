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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.FluentIterable;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;

class LazyDomElement {
  private final WebDriver driver;
  private final By selector;
  private final ElementFilter filter;
  private final Retry retry;

  LazyDomElement(WebDriver driver, By selector) {
    this(driver, selector, Retry._30_SECONDS);
  }

  LazyDomElement(WebDriver driver, By selector, Retry retry) {
    this(driver, selector, ElementFilter.any(), retry);
  }

  private LazyDomElement(WebDriver driver, By selector, ElementFilter filter, Retry retry) {
    this.driver = driver;
    this.selector = selector;
    this.filter = filter;
    this.retry = retry;
  }

  public LazyDomElement withText(final String text) {
    String fullDescription = " with text [" + text + "]";

    return with(new ElementFilter(fullDescription, new Function<Collection<WebElement>, Collection<WebElement>>() {
      @Override
      public Collection<WebElement> apply(Collection<WebElement> stream) {
        return FluentIterable.from(stream).filter(new Predicate<WebElement>() {
          @Override
          public boolean apply(@Nullable WebElement element) {
//            return Objects.equals(element.getText(), text);
            return element.getText().contains(text);
          }
        }).toList();
      }
    }));
  }

  public LazyShould should() {
    return new LazyShould(this, Retry._30_SECONDS, true);
  }

  public void fill(final CharSequence text) {
    execute("fill(" + text + ")", new Consumer<WebElement>() {
      @Override
      public void accept(WebElement element) {
        element.clear();
        element.sendKeys(text);
      }
    });
  }

  public void pressEnter() {
    execute("pressEnter", new Consumer<WebElement>() {
      @Override
      public void accept(WebElement element) {
        element.sendKeys(Keys.ENTER);
      }
    });
  }

  public void select(final String text) {
    executeSelect("select(" + text + ")", new Consumer<Select>() {
      @Override
      public void accept(Select select) {
        select.selectByVisibleText(text);
      }
    });
  }

  public void executeSelect(String description, final Consumer<Select> selectOnElement) {
    execute(description, new Consumer<WebElement>() {
      @Override
      public void accept(WebElement element) {
        selectOnElement.accept(new Select(element));
      }
    });
  }

  public void click() {
    execute("click", new Consumer<WebElement>() {
      @Override
      public void accept(WebElement element) {
        new Actions(driver).moveToElement(element);
        element.click();
      }
    });
  }

  public void check() {
    execute("check", new Consumer<WebElement>() {
      @Override
      public void accept(WebElement element) {
        if (!element.isSelected()) {
          element.click();
        }
      }
    });
  }

  public void execute(Consumer<WebElement> action) {
    execute("execute(" + action + ")", action);
  }

  private LazyDomElement with(ElementFilter filter) {
    return new LazyDomElement(driver, selector, this.filter.and(filter), retry);
  }

  private void execute(String message, Consumer<WebElement> action) {
    System.out.println(" - " + Text.toString(selector) + filter.getDescription() + "." + message);

    Supplier<Optional<WebElement>> findOne = new Supplier<Optional<WebElement>>() {
      @Override
      public Optional<WebElement> get() {
        List<WebElement> elements = stream();
        if (elements.isEmpty()) {
          return Optional.empty();
        }
        return Optional.of(elements.get(0));
      }
    };

    try {
      retry.execute(findOne, action);
    } catch (NoSuchElementException e) {
      throw new AssertionError("Element not found: " + Text.toString(selector));
    }
  }

  List<WebElement> stream() {
    return FluentIterable.from(filter.getFilter().apply(driver.findElements(selector))).toList();
  }

  @Override
  public String toString() {
    return Text.toString(selector) + filter.getDescription();
  }
}
