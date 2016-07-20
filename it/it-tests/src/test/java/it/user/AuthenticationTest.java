/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package it.user;

import com.codeborne.selenide.Condition;
import com.sonar.orchestrator.Orchestrator;
import it.Category4Suite;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import pageobjects.LoginPage;
import pageobjects.Navigation;

public class AuthenticationTest {

  @ClassRule
  public static Orchestrator ORCHESTRATOR = Category4Suite.ORCHESTRATOR;

  @Rule
  public Navigation nav = Navigation.get(ORCHESTRATOR);

  @Test
  public void log_in_with_correct_credentials_then_log_out() {
    nav.shouldNotBeLoggedIn();

    Navigation page = nav.logIn().submitCredentials("admin", "admin");
    page.getRightBar().shouldHave(Condition.text("Administrator"));
    nav.shouldBeLoggedIn();

    nav.logOut();
    nav.shouldNotBeLoggedIn();
  }

  @Test
  public void log_in_with_wrong_credentials() {
    LoginPage page = nav
      .logIn()
      .submitWrongCredentials("admin", "wrong");
    page.getErrorMessage().shouldHave(Condition.text("Authentication failed"));

    nav.openHomepage();
    nav.shouldNotBeLoggedIn();
  }
}
