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
package org.sonarqube.qa.util.pageobjects.projects;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

public class FacetItem {

  private final SelenideElement elt;

  public FacetItem(SelenideElement elt) {
    this.elt = elt;
  }

  public FacetItem shouldHaveValue(String key, String value) {
    this.elt.$(".facet[data-key=\"" + key + "\"] .facet-stat").shouldHave(Condition.text(value));
    return this;
  }

  public void selectValue(String key) {
    this.elt.$(".facet[data-key=\"" + key + "\"]").click();
  }

  public FacetItem selectOptionItem(String value) {
    SelenideElement selectInput = this.elt.$(".Select-input input");
    selectInput.val(value);
    this.elt.$("div.Select-option.is-focused").should(Condition.exist);
    selectInput.pressEnter();
    return this;
  }
}
