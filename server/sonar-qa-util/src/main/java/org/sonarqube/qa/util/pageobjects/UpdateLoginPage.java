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

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class UpdateLoginPage extends Navigation {

  public UpdateLoginPage shouldHaveProviderName(String providerName) {
    $(".js-provider-name").shouldHave(text(providerName));
    return this;
  }

  public UpdateLoginPage shouldHaveNewAccount(String login) {
    $(".js-new-account").shouldHave(text(login));
    return this;
  }

  public UpdateLoginPage shouldHaveOldLogin(String oldLogin) {
    $(".js-old-login").shouldHave(text(oldLogin));
    return this;
  }

  public UpdateLoginPage shouldHaveOldOrganizationKey(String oldOrganizationKey) {
    $(".js-old-organization-key").shouldHave(text(oldOrganizationKey));
    return this;
  }

  public void clickContinue() {
    $(".js-continue").click();
    $(".js-continue").shouldNotBe(visible);
  }

  public void clickCancel() {
    $(".js-cancel").click();
    $(".js-cancel").shouldNotBe(visible);
  }

}
