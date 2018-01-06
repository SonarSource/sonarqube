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

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

public class MembersPage {

  public MembersPage() {
    Selenide.$(".navbar-tabs a.active").shouldBe(Condition.visible).shouldHave(Condition.text("Members"));
  }

  public ElementsCollection getMembers() {
    return Selenide.$$("table.data tr");
  }

  public MemberItem getMembersByIdx(Integer idx) {
    return new MemberItem(getMembers().get(idx));
  }

  public MembersPage shouldHaveTotal(int total) {
    Selenide.$(".panel-vertical > span > strong").shouldHave(Condition.text(String.valueOf(total)));
    return this;
  }

  public MembersPage searchForMember(String query) {
    Selenide.$(".page .search-box-input").shouldBe(Condition.visible).val("").sendKeys(query);
    return this;
  }

  public MembersPage canAddMember() {
    Selenide.$(".page-actions").shouldBe(Condition.visible);
    return this;
  }

  public MembersPage canNotAddMember() {
    Selenide.$(".page-actions").shouldNot(Condition.exist);
    return this;
  }

  public MembersPage addMember(String login) {
    this.canAddMember();
    Selenide.$(".page-actions button").click();

    SelenideElement modal = getModal("Add user");
    SelenideElement input = modal.$(".Select-input input");
    input.val(login);
    modal.$("div.Select-option.is-focused").should(Condition.exist);
    input.pressEnter();
    modal.$("button[type='submit']").click();
    return this;
  }

  private static SelenideElement getModal(String title) {
    Selenide.$(".modal-head").should(Condition.exist).shouldHave(Condition.text(title));
    SelenideElement form = Selenide.$(".ReactModalPortal form");
    form.should(Condition.exist);
    return form;
  }
}
