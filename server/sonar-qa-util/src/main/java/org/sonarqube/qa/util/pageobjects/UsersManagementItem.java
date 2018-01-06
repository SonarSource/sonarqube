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
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class UsersManagementItem {
  private final SelenideElement elt;

  public UsersManagementItem(SelenideElement elt) {
    this.elt = elt;
  }

  public UsersManagementItem hasTokensCount(Integer count) {
    this.elt.$(".js-user-tokens").should(Condition.exist).parent().shouldHave(Condition.text(count.toString()));
    return this;
  }

  public UsersManagementItem generateToken(String name) {
    this.openTokenModal();
    $(".modal #generate-token-form input").should(Condition.exist).sendKeys(name);
    $(".modal #generate-token-form .js-generate-token").should(Condition.exist).click();
    $(".modal code").should(Condition.exist);
    getTokenRow(name).should(Condition.exist);
    closeModal();
    return this;
  }

  public UsersManagementItem revokeToken(String name) {
    this.openTokenModal();
    SelenideElement tokenRow = getTokenRow(name).should(Condition.exist);
    tokenRow.$("button").should(Condition.exist).shouldHave(Condition.text("Revoke")).click();
    tokenRow.$("button").shouldHave(Condition.text("Sure?")).click();
    getTokenRow(name).shouldNot(Condition.exist);
    closeModal();
    return this;
  }

  public UsersManagementItem changePassword(String oldPwd, String newPwd) {
    this.elt.$("button.dropdown-toggle").should(Condition.exist).click();
    this.elt.$(".js-user-change-password").shouldBe(Condition.visible).click();
    isModalOpen("Change password");
    $(".modal #old-user-password").should(Condition.exist).sendKeys(oldPwd);
    $(".modal #user-password").should(Condition.exist).sendKeys(newPwd);
    $(".modal #confirm-user-password").should(Condition.exist).sendKeys(newPwd);
    $(".modal .js-confirm").click();
    $(".modal").shouldNot(Condition.exist);
    return this;
  }

  public UsersManagementItem deactivateUser() {
    this.elt.$("button.dropdown-toggle").should(Condition.exist).click();
    this.elt.$(".js-user-deactivate").should(Condition.exist).click();
    isModalOpen("Deactivate User");
    $(".modal .js-confirm").should(Condition.exist).click();
    return this;
  }

  private void openTokenModal() {
    if (!$(".modal").exists()) {
      this.elt.$(".js-user-tokens").should(Condition.exist).click();
    }
    isModalOpen("Tokens");
  }

  private static void closeModal() {
    $(".modal .js-modal-close").should(Condition.exist).click();
  }

  private static void isModalOpen(String title) {
    $(".modal .modal-head").should(Condition.exist).shouldHave(Condition.text(title));
  }

  private static SelenideElement getTokenRow(String name) {
    return $$(".modal tr").findBy(Condition.text(name));
  }
}
