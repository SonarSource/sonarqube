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
package org.sonarqube.qa.util.pageobjects;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.NoSuchElementException;

public class ProjectLinkItem {

  private final SelenideElement elt;

  public ProjectLinkItem(SelenideElement elt) {
    this.elt = elt;
  }

  public SelenideElement getName() {
    return elt.$(".js-name");
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
    return elt.$(".js-delete-button");
  }
}
