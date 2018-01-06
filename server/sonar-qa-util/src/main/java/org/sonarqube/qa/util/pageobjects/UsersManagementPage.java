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

public class UsersManagementPage {
  public UsersManagementPage() {
    $("#users-page").shouldBe(Condition.visible);
  }

  public UsersManagementPage hasUsersCount(Integer count) {
    $$(".js-user-login").shouldHaveSize(count);
    return this;
  }

  public UsersManagementItem getUser(String login) {
    SelenideElement elt = $$(".js-user-login").findBy(Condition.text(login)).should(Condition.exist);
    return new UsersManagementItem(elt.parent().parent().parent());
  }

  public UsersManagementPage createUser(String login) {
    $("#users-create").should(Condition.exist).click();
    $(".modal .modal-head").should(Condition.exist).shouldHave(Condition.text("Create User"));
    $(".modal #create-user-login").should(Condition.exist).sendKeys(login);
    $(".modal #create-user-name").should(Condition.exist).sendKeys("Name of " + login);
    $(".modal #create-user-password").should(Condition.exist).sendKeys(login);
    $(".modal .js-confirm").should(Condition.exist).click();
    return this;
  }
}
