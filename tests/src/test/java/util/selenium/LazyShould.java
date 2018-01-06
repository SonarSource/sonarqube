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
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.FluentIterable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.openqa.selenium.WebElement;

class LazyShould {
  private final LazyDomElement element;
  private final Retry retry;
  private final boolean ok;

  LazyShould(LazyDomElement element, Retry retry, boolean ok) {
    this.element = element;
    this.retry = retry;
    this.ok = ok;
  }

  public LazyShould beDisplayed() {
    return verify(
      isOrNot("displayed"),
      new Predicate<List<WebElement>>() {
        @Override
        public boolean apply(List<WebElement> elements) {
          return !elements.isEmpty() && FluentIterable.from(elements).allMatch(new Predicate<WebElement>() {
            @Override
            public boolean apply(WebElement element) {
              return element.isDisplayed();
            }
          });
        }
      },
      new Function<List<WebElement>, String>() {
        @Override
        public String apply(List<WebElement> elements) {
          return "It is " + statuses(elements, new Function<WebElement, String>() {
            @Override
            public String apply(WebElement element) {
              return displayedStatus(element);
            }
          });
        }
      });
  }

  public LazyShould match(final Pattern regexp) {
    return verify(
      doesOrNot("match") + " (" + regexp.pattern() + ")",
      new Predicate<List<WebElement>>() {
        @Override
        public boolean apply(List<WebElement> elements) {
          return !elements.isEmpty() && FluentIterable.from(elements).anyMatch(new Predicate<WebElement>() {
            @Override
            public boolean apply(WebElement element) {
              return regexp.matcher(WebElementHelper.text(element)).matches();
            }
          });
        }
      },
      new Function<List<WebElement>, String>() {
        @Override
        public String apply(List<WebElement> elements) {
          return "It contains " + statuses(elements, new Function<WebElement, String>() {
            @Nullable
            @Override
            public String apply(@Nullable WebElement element) {
              return WebElementHelper.text(element);
            }
          });
        }
      });
  }

  public LazyShould contain(final String text) {
    return verify(
      doesOrNot("contain") + "(" + text + ")",
      new Predicate<List<WebElement>>() {
        @Override
        public boolean apply(List<WebElement> elements) {
          return FluentIterable.from(elements).anyMatch(new Predicate<WebElement>() {
            @Override
            public boolean apply(@Nullable WebElement element) {
              if (text.startsWith("exact:")) {
                return WebElementHelper.text(element).equals(text.substring(6));
              }
              return WebElementHelper.text(element).contains(text);
            }
          });
        }
      },
      new Function<List<WebElement>, String>() {
        @Override
        public String apply(List<WebElement> elements) {
          return "It contains " + statuses(elements, new Function<WebElement, String>() {
            @Override
            public String apply(WebElement element) {
              return WebElementHelper.text(element);
            }
          });
        }
      });
  }

  public LazyShould exist() {
    return verify(
      doesOrNot("exist"),
      new Predicate<List<WebElement>>() {
        @Override
        public boolean apply(List<WebElement> elements) {
          return !elements.isEmpty();
        }
      },
      new Function<List<WebElement>, String>() {
        @Override
        public String apply(List<WebElement> elements) {
          return "It contains " + Text.plural(elements.size(), "element");
        }
      });
  }

  private static String displayedStatus(WebElement element) {
    return element.isDisplayed() ? "displayed" : "not displayed";
  }

  private LazyShould verify(String message, Predicate<List<WebElement>> predicate, Function<List<WebElement>, String> toErrorMessage) {
    String verification = "verify that " + element + " " + message;
    System.out.println("   -> " + verification);

    try {
      if (!retry.verify(new Supplier<List<WebElement>>() {
        @Override
        public List<WebElement> get() {
          return LazyShould.this.findElements();
        }
      }, ok ? predicate : Predicates.not(predicate))) {
        throw Failure.create("Failed to " + verification + ". " + toErrorMessage.apply(findElements()));
      }
    } catch (NoSuchElementException e) {
      throw Failure.create("Element not found. Failed to " + verification);
    }

    return ok ? this : not();
  }

  private List<WebElement> findElements() {
    return element.stream();
  }

  private static String statuses(List<WebElement> elements, Function<WebElement, String> toStatus) {
    return "(" + FluentIterable.from(elements).transform(toStatus).join(Joiner.on(";")) + ")";
  }

  public LazyShould not() {
    return new LazyShould(element, retry, !ok);
  }

  private String doesOrNot(String verb) {
    return Text.doesOrNot(!ok, verb);
  }

  private String isOrNot(String state) {
    return Text.isOrNot(!ok, state);
  }
}
