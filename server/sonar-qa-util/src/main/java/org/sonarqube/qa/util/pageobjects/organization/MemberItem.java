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
package org.sonarqube.qa.util.pageobjects.organization;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

public class MemberItem {
  private final SelenideElement elt;

  public MemberItem(SelenideElement elt) {
    this.elt = elt;
  }

  public MemberItem shouldBeNamed(String login, String name) {
    ElementsCollection tds = this.elt.$$("td");
    tds.get(1).$("strong").shouldHave(Condition.text(name));
    tds.get(1).$("span").shouldHave(Condition.text(login));
    return this;
  }

  public MemberItem shouldHaveGroups(Integer groups) {
    ElementsCollection tds = this.elt.$$("td");
    tds.get(2).should(Condition.exist);
    tds.get(2).shouldHave(Condition.text(groups.toString()));
    return this;
  }

  public MemberItem shouldNotHaveActions() {
    this.elt.$$("td").shouldHave(CollectionCondition.sizeLessThan(3));
    return this;
  }

  public MemberItem removeMembership() {
    ElementsCollection tds = this.elt.$$("td");
    tds.shouldHave(CollectionCondition.sizeGreaterThan(3));
    SelenideElement actionTd = tds.get(3);
    actionTd.$("button").should(Condition.exist).click();
    actionTd.$$(".dropdown-menu > li").get(2).shouldBe(Condition.visible).click();
    SelenideElement modal = getModal("Remove user");
    modal.$("button.button-red").shouldBe(Condition.visible).click();
    return this;
  }

  public MemberItem manageGroupsOpen() {
    ElementsCollection tds = this.elt.$$("td");
    tds.shouldHave(CollectionCondition.sizeGreaterThan(3));
    SelenideElement actionTd = tds.get(3);
    actionTd.$("button").should(Condition.exist).click();
    actionTd.$$(".dropdown-menu > li").get(0).shouldBe(Condition.visible).click();
    getModal("Manage groups");
    return this;
  }

  public MemberItem manageGroupsSelect(String group) {
    SelenideElement modal = getModal("Manage groups");
    modal.$$("li").find(Condition.text(group)).shouldBe(Condition.visible).click();
    return this;
  }

  public MemberItem manageGroupsSave() {
    SelenideElement modal = getModal("Manage groups");
    modal.$("button[type='submit']").shouldBe(Condition.visible).click();
    return this;
  }

  private static SelenideElement getModal(String title) {
    Selenide.$(".modal-head").should(Condition.exist).shouldHave(Condition.text(title));
    SelenideElement form = Selenide.$(".ReactModalPortal form");
    form.should(Condition.exist);
    return form;
  }
}
