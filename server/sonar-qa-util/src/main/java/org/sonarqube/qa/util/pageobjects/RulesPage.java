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
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class RulesPage extends Navigation {

  public RulesPage() {
    $(By.cssSelector(".coding-rules")).should(exist);
  }

  public int getTotal() {
    // warning - number is localized
    return Integer.parseInt($("#coding-rules-total").text());
  }

  public ElementsCollection getSelectedFacetItems(String facetName) {
    SelenideElement facet = $(getFacetSelector(facetName)).shouldBe(visible);
    return facet.$$(".js-facet.active");
  }

  public RulesPage shouldHaveTotalRules(Integer total) {
    $("#coding-rules-total").shouldHave(Condition.text(total.toString()));
    return this;
  }

  public RulesPage shouldDisplayRules(String... ruleNames) {
    for (String ruleName : ruleNames) {
      getRulesCollection().findBy(text(ruleName)).shouldBe(visible);
    }
    return this;
  }

  public RulesPage shouldNotDisplayRules(String... ruleNames) {
    for (String ruleName : ruleNames) {
      getRulesCollection().findBy(text(ruleName)).shouldNotBe(visible);
    }
    return this;
  }

  public RulesPage shouldDisplayRuleWithLanguage(String ruleName, String languageName) {
    getRulesCollection()
      .findBy(Condition.and("", text(ruleName), text(languageName)))
      .shouldBe(visible);
    return this;
  }

  public RulesPage shouldNotDisplayRuleWithLanguage(String ruleName, String languageName) {
    getRulesCollection()
      .findBy(Condition.and("", text(ruleName), text(languageName)))
      .shouldNotBe(visible);
    return this;
  }

  public RulesPage openFacet(String facet) {
    $(getFacetSelector(facet) + " .js-facet-toggle").click();
    return this;
  }

  public RulesPage selectFacetItemByText(String facet, String itemText) {
    $$(getFacetSelector(facet) + " .js-facet")
      .findBy(Condition.text(itemText)).click();
    return this;
  }

  public RulesPage selectFacetItem(String facet, String value) {
    $(".search-navigator-facet-box[data-property=\"" + facet + "\"] .js-facet[data-value=\"" + value + "\"]").click();
    return this;
  }

  public RulesPage selectInactive() {
    $(getFacetSelector("qprofile") + " .search-navigator-facet.active .js-inactive").click();
    return this;
  }

  public RulesPage shouldHaveDisabledFacet(String facet) {
    $(".search-navigator-facet-box-forbidden[data-property=\"" + facet + "\"]").shouldBe(visible);
    return this;
  }

  public RulesPage shouldNotHaveDisabledFacet(String facet) {
    $(".search-navigator-facet-box-forbidden[data-property=\"" + facet + "\"]").shouldNotBe(visible);
    return this;
  }

  public RuleDetails openFirstRule() {
    $$(".js-rule").first().click();
    $(".coding-rules-details").shouldBe(visible);
    return new RuleDetails();
  }

  public RuleItem takeRuleByName(String ruleName) {
    return new RuleItem(getRulesCollection().findBy(text(ruleName)));
  }

  public RuleItem takeRuleByName(String ruleName, int index) {
    return new RuleItem(getRulesCollection().filterBy(text(ruleName)).get(index));
  }

  public RulesPage search(String query) {
    $(".search-navigator-facet-query .search-box-input").val(query).pressEnter();
    return this;
  }

  public RulesPage clearAllFilters() {
    $(".js-new-search").click();
    return this;
  }

  public RulesPage closeDetails() {
    $(".js-back").click();
    $(".coding-rules-details").shouldNotBe(visible);
    return this;
  }

  public RulesPage activateOnlyRule() {
    $$(".coding-rules-detail-quality-profile-activate").shouldHaveSize(1).first().click();
    $(".modal").shouldBe(visible);
    $("#coding-rules-quality-profile-activation-activate").click();
    $(".modal").shouldNotBe(visible);
    $(".coding-rules-detail-quality-profile-activate").shouldNotBe(visible);
    return this;
  }

  public RulesPage deactivateOnlyRule() {
    $$(".coding-rules-detail-quality-profile-deactivate").shouldHaveSize(1).first().click();
    $("button[data-confirm=\"yes\"]").click();
    $(".coding-rules-detail-quality-profile-deactivate").shouldNotBe(visible);
    return this;
  }

  public RulesPage onlyRuleShouldBeActivated() {
    $$(".coding-rules-detail-quality-profile-deactivate").shouldHaveSize(1);
    $$(".coding-rules-detail-quality-profile-activate").shouldHaveSize(0);
    return this;
  }

  private static String getFacetSelector(String facet) {
    return ".search-navigator-facet-box[data-property=\"" + facet + "\"]";
  }

  private static ElementsCollection getRulesCollection() {
    return $$(".coding-rule");
  }
}
