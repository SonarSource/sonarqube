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

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import java.util.Arrays;
import org.openqa.selenium.By;

import static org.assertj.core.api.Assertions.assertThat;


public class ProjectDashboardPage {

  public ProjectDashboardPage() {
    Selenide.$(".overview").shouldBe(Condition.visible);
  }

  public SelenideElement getLinesOfCode() {
    SelenideElement element = Selenide.$("#overview-ncloc");
    element.shouldBe(Condition.visible);
    return element;
  }

  public SelenideElement getLanguageDistribution() {
    SelenideElement element = Selenide.$("#overview-language-distribution");
    element.shouldBe(Condition.visible);
    return element;
  }

  public SelenideElement getOverviewMeasure(String measure) {
    ElementsCollection measures = Selenide.$$(".overview-domain-measure");
    return measures.find(Condition.text(measure)).shouldBe(Condition.visible);
  }

  private static SelenideElement getTagsMeta() {
    SelenideElement element = Selenide.$(".overview-meta-tags");
    element.shouldBe(Condition.visible);
    return element;
  }

  public ProjectDashboardPage shouldHaveTags(String... tags) {
    String tagsList = String.join(", ", Arrays.asList(tags));
    getTagsMeta().$(".tags-list > span").should(Condition.text(tagsList));
    return this;
  }

  public ProjectDashboardPage shouldNotBeEditable() {
    SelenideElement tagsElem = getTagsMeta();
    tagsElem.$("button").shouldNot(Condition.exist);
    tagsElem.$("div.multi-select").shouldNot(Condition.exist);
    return this;
  }

  public ProjectDashboardPage shouldBeEditable() {
    SelenideElement tagsElem = getTagsMeta();
    tagsElem.$("button").shouldBe(Condition.visible);
    return this;
  }

  public ProjectDashboardPage openTagEditor() {
    SelenideElement tagsElem = getTagsMeta();
    tagsElem.$("button").shouldBe(Condition.visible).click();
    tagsElem.$("div.multi-select").shouldBe(Condition.visible);
    return this;
  }

  public SelenideElement getTagAtIdx(Integer idx) {
    SelenideElement tagsElem = getTagsMeta();
    tagsElem.$("div.multi-select").shouldBe(Condition.visible);
    return tagsElem.$$("ul.menu a").get(idx);
  }

  public ProjectDashboardPage sendKeysToTagsInput(CharSequence... charSequences) {
    SelenideElement tagsInput = getTagsMeta().find("input");
    tagsInput.sendKeys(charSequences);
    return this;
  }

  public ProjectDashboardPage hasQualityGateLink(String name, String link) {
    SelenideElement elem = Selenide.$$(".overview-meta-card")
      .findBy(Condition.text("Quality Gate")).should(Condition.exist)
      .find(By.linkText(name)).should(Condition.exist);
    assertThat(elem.attr("href")).endsWith(link);
    return this;
  }
}
