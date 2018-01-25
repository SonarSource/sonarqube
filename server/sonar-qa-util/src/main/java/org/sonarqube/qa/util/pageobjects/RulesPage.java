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
    SelenideElement facet = $(".search-navigator-facet-box[data-property='"+ facetName+"']").shouldBe(visible);
    return facet.$$(".facet.active");
  }

  public RulesPage shouldHaveTotalRules(Integer total) {
    $(".js-page-counter-total").shouldHave(Condition.text(total.toString()));
    return this;
  }

  public RulesPage openFacet(String facet) {
    $(".search-navigator-facet-box[data-property=\"" + facet + "\"] .search-navigator-facet-header a").click();
    return this;
  }

  public RulesPage selectFacetItemByText(String facet, String itemText) {
    $$(".search-navigator-facet-box[data-property=\"" + facet + "\"] .facet")
      .findBy(Condition.text(itemText)).click();
    return this;
  }

  public RuleDetails openFirstRule() {
    $$(".coding-rule").first().find(".coding-rule-title a").click();
    $(".coding-rule-details").shouldBe(visible);
    return new RuleDetails();
  }

}
