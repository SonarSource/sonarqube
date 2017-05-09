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
package pageobjects;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.page;

public class LoginPage {

  public LoginPage() {
    $("#login_form").should(Condition.exist);
  }

  public Navigation submitCredentials(String login, String password) {
    return submitCredentials(login, password, Navigation.class);
  }

  public Navigation asAdmin() {
    return submitCredentials("admin", "admin");
  }

  public Navigation useOAuth2() {
    $(".oauth-providers a").click();
    return page(Navigation.class);
  }

  public LoginPage submitWrongCredentials(String login, String password) {
    $("#login").val(login);
    $("#password").val(password);
    $(By.name("commit")).click();
    return page(LoginPage.class);
  }

  public SelenideElement getErrorMessage() {
    return $(".process-spinner-failed");
  }

  private <T> T submitCredentials(String login, String password, Class<T> expectedResultPage) {
    $("#login").val(login);
    $("#password").val(password);
    $(By.name("commit")).click();
    $("#login").should(Condition.disappear);
    return page(expectedResultPage);
  }
}
