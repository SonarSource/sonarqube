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

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class RulesPage extends Navigation {

  public RulesPage() {
    $("#coding-rules-page").should(exist);
  }

  public int getTotal() {
    // warning - number is localized
    return Integer.parseInt($("#coding-rules-total").text());
  }

  public ElementsCollection getSelectedFacetItems(String facetName) {
    return getFacetElement(facetName).$$(".facet.active");
  }

  public RulesPage shouldHaveTotalRules(Integer total) {
    $(".js-page-counter-total").shouldHave(Condition.text(total.toString()));
    return this;
  }

  public RulesPage shouldDisplayRules(String... ruleKeys) {
    for (String key : ruleKeys) {
      getRuleElement(key).shouldBe(visible);
    }
    return this;
  }

  public RulesPage shouldNotDisplayRules(String... ruleKeys) {
    for (String key : ruleKeys) {
      getRuleElement(key).shouldNotBe(visible);
    }
    return this;
  }

  public RulesPage openFacet(String facet) {
    getFacetElement(facet).$(".search-navigator-facet-header a").click();
    return this;
  }

  public RulesPage selectFacetItem(String facet, String value) {
    getFacetElement(facet).$(".facet[data-facet=\"" + value + "\"]").click();
    return this;
  }

  public RulesPage selectInactive() {
    getFacetElement("profile").$(".active .js-inactive").click();
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
    $$(".coding-rule-title a").first().click();
    return new RuleDetails();
  }

  public RuleItem takeRule(String ruleKey) {
    return new RuleItem(getRuleElement(ruleKey));
  }

  public RulesPage search(String query) {
    $("#coding-rules-search .search-box-input").val(query);
    return this;
  }

  public RulesPage clearAllFilters() {
    $("#coding-rules-clear-all-filters").click();
    return this;
  }

  public RulesPage closeDetails() {
    $(".js-back").click();
    $(".coding-rule-details").shouldNotBe(visible);
    return this;
  }

  public RulesPage activateRule(String ruleKey) {
    getRuleElement(ruleKey).$(".coding-rules-detail-quality-profile-activate").click();
    $(".modal").shouldBe(visible);
    $(".modal button").click();
    $(".modal").shouldNotBe(visible);
    getRuleElement(ruleKey).$(".coding-rules-detail-quality-profile-activate").shouldNotBe(visible);
    return this;
  }

  public RulesPage deactivateRule(String ruleKey) {
    getRuleElement(ruleKey).$(".coding-rules-detail-quality-profile-deactivate").click();
    $(".modal button").click();
    getRuleElement(ruleKey).$(".coding-rules-detail-quality-profile-deactivate").shouldNotBe(visible);
    return this;
  }

  private static SelenideElement getRuleElement(String key) {
    return $(".coding-rule[data-rule=\"" + key + "\"]");
  }

  private static SelenideElement getFacetElement(String facet) {
    return $(".search-navigator-facet-box[data-property=\"" + facet + "\"]");
  }

}
