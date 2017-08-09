/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.pageobjects.measures;
import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.Keys;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

public class MeasuresPage {
  public MeasuresPage() {
    $("#component-measures").should(exist);
  }

  public MeasuresPage displayBubbleChart(String title) {
    SelenideElement bubblechart = $("#component-measures .measure-overview-bubble-chart");
    bubblechart.$(".measure-overview-bubble-chart-title").shouldHave(text(title));
    return this;
  }

  public MeasuresPage measureHasValue(String measure, Integer value) {
    SelenideElement sidebar = this.getSideBar();
    sidebar.$("#measure-" + measure + "-name").should(exist);
    sidebar.$("#measure-" + measure + "-value").should(exist).shouldHave(text(value.toString()));
    return this;
  }

  public MeasuresPage measureHasLeak(String measure, Integer value) {
    SelenideElement sidebar = this.getSideBar();
    sidebar.$("#measure-" + measure + "-name").should(exist);
    sidebar.$("#measure-" + measure + "-leak").should(exist).shouldHave(text(value.toString()));
    return this;
  }

  public MeasuresPage breadcrumbsShouldHave(String item) {
    $(".layout-page-header-panel .measure-breadcrumbs").shouldHave(text(item));
    return this;
  }

  public MeasuresPage breadcrumbsShouldNotHave(String item) {
    $(".layout-page-header-panel .measure-breadcrumbs").shouldNotHave(text(item));
    return this;
  }

  public MeasuresPage backShortcut() {
    $(".layout-page-header-panel").sendKeys(Keys.LEFT);
    return this;
  }

  public MeasuresPage switchView(String view) {
    SelenideElement select = $(".measure-view-select").should(exist);
    select.click();
    select.$(".Select-menu-outer").should(exist)
      .$$(".Select-option").shouldHave(CollectionCondition.sizeGreaterThan(1))
      .find(text(view)).should(exist).click();
    return this;
  }

  public MeasureContent openMeasureContent(String measure) {
    SelenideElement sidebar = this.getSideBar();
    SelenideElement facetItem = sidebar.$("#measure-" + measure + "-name").should(exist);
    facetItem.click();
    MeasureContent content = new MeasureContent($("#component-measures .layout-page-main-inner").should(exist));
    content.shouldHaveTitle(facetItem.getText());
    return content;
  }

  private SelenideElement getSideBar() {
    return $("#component-measures .layout-page-side").should(exist);
  }
}
