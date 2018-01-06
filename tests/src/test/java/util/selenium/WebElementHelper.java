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
