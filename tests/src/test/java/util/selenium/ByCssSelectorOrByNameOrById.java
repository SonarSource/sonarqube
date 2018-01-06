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

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.FindsByCssSelector;
import org.openqa.selenium.internal.FindsById;
import org.openqa.selenium.internal.FindsByName;

public class ByCssSelectorOrByNameOrById extends By implements Serializable {
  private static final long serialVersionUID = -3910258723099459239L;

  private final String selector;

  public ByCssSelectorOrByNameOrById(String selector) {
    this.selector = selector;
  }

  @Override
  public WebElement findElement(SearchContext context) {
    WebElement element;

    if (validCssSelector(selector)) {
      element = ((FindsByCssSelector) context).findElementByCssSelector(quoteCss(selector));
      if (element != null) {
        return element;
      }
    }

    element = ((FindsByName) context).findElementByName(selector);
    if (element != null) {
      return element;
    }

    element = ((FindsById) context).findElementById(selector);
    if (element != null) {
      return element;
    }

    return null;
  }

  @Override
  public List<WebElement> findElements(SearchContext context) {
    List<WebElement> elements;

    if (validCssSelector(selector)) {
      elements = ((FindsByCssSelector) context).findElementsByCssSelector(quoteCss(selector));
      if ((elements != null) && (!elements.isEmpty())) {
        return elements;
      }
    }

    elements = ((FindsByName) context).findElementsByName(selector);
    if ((elements != null) && (!elements.isEmpty())) {
      return elements;
    }

    elements = ((FindsById) context).findElementsById(selector);
    if ((elements != null) && (!elements.isEmpty())) {
      return elements;
    }

    return Collections.emptyList();
  }

  protected boolean validCssSelector(String selector) {
    return !selector.endsWith("[]");
  }

  protected String quoteCss(String selector) {
    if (selector.startsWith(".")) {
      return selector;
    }
    if (selector.startsWith("#")) {
      return selector.replaceAll("(\\w)[.]", "$1\\\\.");
    }
    return selector;
  }

  @Override
  public String toString() {
    return selector;
  }
}
