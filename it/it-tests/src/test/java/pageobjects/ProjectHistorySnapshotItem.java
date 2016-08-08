/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package pageobjects;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.NoSuchElementException;

public class ProjectHistorySnapshotItem {

  private final SelenideElement elt;

  public ProjectHistorySnapshotItem(SelenideElement elt) {
    this.elt = elt;
  }

  public SelenideElement getVersionText() {
    return elt.$("td:nth-child(5) table td:nth-child(1)");
  }

  public SelenideElement getType() {
    try {
      return elt.$(".js-type");
    } catch (NoSuchElementException e) {
      return null;
    }
  }

  public SelenideElement getUrl() {
    return elt.$(".js-url");
  }

  public SelenideElement getDeleteButton() {
    return elt.$("td:nth-child(9) input[type=\"submit\"]");
  }

  public void clickDelete() {
    getDeleteButton().click();
  }
}
