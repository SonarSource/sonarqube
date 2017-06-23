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

package org.sonarqube.pageobjects.organization;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class MembersPage {

  public MembersPage() {
    $(".nav-tabs a.active").shouldBe(visible).shouldHave(text("Members"));
  }

  public ElementsCollection getMembers() {
    return $$("table.data tr");
  }

  public MemberItem getMembersByIdx(Integer idx) {
    return new MemberItem(getMembers().get(idx));
  }

  public MembersPage shouldHaveTotal(int total) {
    $(".panel-vertical > span > strong").shouldHave(text(String.valueOf(total)));
    return this;
  }

  public MembersPage searchForMember(String query) {
    $("input.search-box-input").shouldBe(visible).val("").sendKeys(query);
    return this;
  }

  public MembersPage canAddMember() {
    $(".page-actions").shouldBe(visible);
    return this;
  }

  public MembersPage canNotAddMember() {
    $(".page-actions").shouldNot(Condition.exist);
    return this;
  }

  public MembersPage addMember(String login) {
    this.canAddMember();
    $(".page-actions button").click();

    SelenideElement modal = this.getModal("Add user");
    SelenideElement input = modal.$(".Select-input input");
    input.val(login);
    modal.$("div.Select-option.is-focused").should(Condition.exist);
    input.pressEnter();
    modal.$("button[type='submit']").click();
    return this;
  }

  private SelenideElement getModal(String title) {
    $(".modal-head").should(Condition.exist).shouldHave(text(title));
    SelenideElement form = $(".ReactModalPortal form");
    form.should(Condition.exist);
    return form;
  }
}
