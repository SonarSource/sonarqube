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

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.openqa.selenium.Keys;
import org.openqa.selenium.interactions.Actions;

public class MeasuresPage {
  public MeasuresPage() {
    Selenide.$("#component-measures").should(Condition.exist);
  }

  public MeasuresPage displayBubbleChart(String title) {
    SelenideElement bubblechart = Selenide.$("#component-measures .measure-overview-bubble-chart");
    bubblechart.$(".measure-overview-bubble-chart-title").shouldHave(Condition.text(title));
    return this;
  }

  public MeasuresPage measureHasValue(String measure, Integer value) {
    SelenideElement sidebar = getSideBar();
    sidebar.$("#measure-" + measure + "-name").should(Condition.exist);
    sidebar.$("#measure-" + measure + "-value").should(Condition.exist).shouldHave(Condition.text(value.toString()));
    return this;
  }

  public MeasuresPage measureHasLeak(String measure, Integer value) {
    SelenideElement sidebar = getSideBar();
    sidebar.$("#measure-" + measure + "-name").should(Condition.exist);
    sidebar.$("#measure-" + measure + "-leak").should(Condition.exist).shouldHave(Condition.text(value.toString()));
    return this;
  }

  public MeasuresPage breadcrumbsShouldHave(String item) {
    Selenide.$(".layout-page-header-panel .measure-breadcrumbs").shouldHave(Condition.text(item));
    return this;
  }

  public MeasuresPage breadcrumbsShouldNotHave(String item) {
    Selenide.$(".layout-page-header-panel .measure-breadcrumbs").shouldNotHave(Condition.text(item));
    return this;
  }

  public MeasuresPage backShortcut() {
    SelenideElement panel = Selenide.$(".layout-page-header-panel");

    // panel.sendKeys(Keys.LEFT) does not work correctly on Chrome
    // The workaround is to use Actions
    // https://bugs.chromium.org/p/chromedriver/issues/detail?id=35
    Actions actions = new Actions(WebDriverRunner.getWebDriver());
    actions.moveToElement(panel);
    actions.click();
    actions.sendKeys(Keys.LEFT);
    actions.build().perform();
    return this;
  }

  public MeasuresPage switchView(String view) {
    SelenideElement select = Selenide.$(".measure-view-select").should(Condition.exist);
    select.click();
    select.$(".Select-menu-outer").should(Condition.exist)
      .$$(".Select-option").shouldHave(CollectionCondition.sizeGreaterThan(1))
      .find(Condition.text(view)).should(Condition.exist).click();
    return this;
  }

  public MeasuresPage openFacet(String facet) {
    SelenideElement facetBox = Selenide.$$(".search-navigator-facet-box").find(Condition.text(facet));
    if(!facetBox.find("search-navigator-facet-list").isDisplayed()) {
      facetBox.$(".search-navigator-facet-header a").should(Condition.exist).click();
    }
    return this;
  }

  public MeasureContent openMeasureContent(String measure) {
    SelenideElement sidebar = getSideBar();
    SelenideElement facetItem = sidebar.$("#measure-" + measure + "-name");
    facetItem.click();
    MeasureContent content = new MeasureContent(Selenide.$("#component-measures .measure-details-content").should(Condition.exist));
    content.shouldHaveTitle(facetItem.getText());
    return content;
  }

  private static SelenideElement getSideBar() {
    return Selenide.$("#component-measures .layout-page-side").should(Condition.exist);
  }
}
