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
package org.sonarqube.qa.util.pageobjects.measures;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class MeasureContent {
  private final SelenideElement elt;

  public MeasureContent(SelenideElement elt) {
    this.elt = elt;
  }

  public MeasureContent shouldHaveTitle(String title) {
    this.elt.$(".measure-details-header .measure-details-metric").should(Condition.exist).shouldHave(Condition.text(title));
    return this;
  }

  public MeasureContent shouldHaveHeaderValue(String value) {
    this.elt.$(".measure-details-header .measure-details-value").should(Condition.exist).shouldHave(Condition.text(value));
    return this;
  }

  public MeasureContent shouldHaveFile(String path) {
    this.getFiles().find(Condition.text(path)).should(Condition.exist);
    return this;
  }

  public MeasureContent drillDown(String item) {
    this.getFiles().find(Condition.text(item)).should(Condition.exist).find("a").click();
    return this;
  }

  public MeasureContent shouldDisplayCode() {
    this.elt.$(".source-line-code").should(Condition.exist);
    return this;
  }

  private ElementsCollection getFiles() {
    return this.elt.$$(".measure-details-component-row");
  }
}
