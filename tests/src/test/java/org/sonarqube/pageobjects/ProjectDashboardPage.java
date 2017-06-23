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
package org.sonarqube.pageobjects;

import com.codeborne.selenide.SelenideElement;
import java.util.Arrays;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hasText;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class ProjectDashboardPage {

  public ProjectDashboardPage() {
    $(".overview").shouldBe(visible);
  }

  public SelenideElement getLinesOfCode() {
    SelenideElement element = $("#overview-ncloc");
    element.shouldBe(visible);
    return element;
  }

  public SelenideElement getLanguageDistribution() {
    SelenideElement element = $("#overview-language-distribution");
    element.shouldBe(visible);
    return element;
  }

  private SelenideElement getTagsMeta() {
    SelenideElement element = $(".overview-meta-tags");
    element.shouldBe(visible);
    return element;
  }

  public ProjectDashboardPage shouldHaveTags(String... tags) {
    String tagsList = String.join(", ", Arrays.asList(tags));
    this.getTagsMeta().$(".tags-list > span").should(hasText(tagsList));
    return this;
  }

  public ProjectDashboardPage shouldNotBeEditable() {
    SelenideElement tagsElem = this.getTagsMeta();
    tagsElem.$("button").shouldNot(exist);
    tagsElem.$("div.multi-select").shouldNot(exist);
    return this;
  }

  public ProjectDashboardPage shouldBeEditable() {
    SelenideElement tagsElem = this.getTagsMeta();
    tagsElem.$("button").shouldBe(visible);
    return this;
  }

  public ProjectDashboardPage openTagEditor() {
    SelenideElement tagsElem = this.getTagsMeta();
    tagsElem.$("button").shouldBe(visible).click();
    tagsElem.$("div.multi-select").shouldBe(visible);
    return this;
  }

  public SelenideElement getTagAtIdx(Integer idx) {
    SelenideElement tagsElem = this.getTagsMeta();
    tagsElem.$("div.multi-select").shouldBe(visible);
    return tagsElem.$$("ul.menu a").get(idx);
  }

  public ProjectDashboardPage sendKeysToTagsInput(CharSequence... charSequences) {
    SelenideElement tagsInput = this.getTagsMeta().find("input");
    tagsInput.sendKeys(charSequences);
    return this;
  }
}
