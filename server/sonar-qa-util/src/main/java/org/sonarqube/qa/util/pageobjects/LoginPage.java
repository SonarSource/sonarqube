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
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

public class LoginPage {

  public LoginPage() {
    Selenide.$("#login_form").should(Condition.exist);
  }

  /**
   * The password is the same as the login.
   */
  public Navigation submitCredentials(String login) {
    return submitCredentials(login, login, Navigation.class);
  }

  public Navigation submitCredentials(String login, String password) {
    return submitCredentials(login, password, Navigation.class);
  }

  public Navigation useOAuth2() {
    Selenide.$(".oauth-providers a").click();
    return Selenide.page(Navigation.class);
  }

  public LoginPage submitWrongCredentials(String login, String password) {
    Selenide.$("#login").val(login);
    Selenide.$("#password").val(password);
    Selenide.$(By.name("commit")).click();
    return Selenide.page(LoginPage.class);
  }

  public SelenideElement getErrorMessage() {
    return Selenide.$(".process-spinner-failed");
  }

  private static <T> T submitCredentials(String login, String password, Class<T> expectedResultPage) {
    Selenide.$("#login").val(login);
    Selenide.$("#password").val(password);
    Selenide.$(By.name("commit")).click();
    Selenide.$("#login").should(Condition.disappear);
    return Selenide.page(expectedResultPage);
  }
}
