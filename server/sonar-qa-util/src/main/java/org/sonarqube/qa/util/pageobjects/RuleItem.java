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

import static com.codeborne.selenide.Condition.visible;

public class RuleItem {

  private final SelenideElement elt;

  RuleItem(SelenideElement elt) {
    this.elt = elt;
  }

  public RuleItem filterSimilarRules(String field) {
    elt.$(".js-rule-filter").click();
    elt.$(".dropdown-menu a[data-field=\"" + field + "\"]").click();
    return this;
  }

  public RuleDetails open() {
    elt.$(".coding-rule-title a").click();
    return new RuleDetails();
  }

  public RuleItem shouldDisplayDeactivate() {
    elt.$(".coding-rules-detail-quality-profile-deactivate").shouldBe(visible);
    return this;
  }

}
