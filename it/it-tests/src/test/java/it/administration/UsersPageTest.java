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
package it.administration;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category1Suite;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import util.QaOnly;
import util.selenium.SeleneseTest;

@Category(QaOnly.class)
public class UsersPageTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Test
  public void generate_and_revoke_user_token() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("generate_and_revoke_user_token",
      "/administration/UsersPageTest/generate_and_revoke_user_token.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void admin_should_change_its_own_password() throws Exception {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("admin_should_change_its_own_password",
      "/administration/UsersPageTest/admin_should_change_its_own_password.html"
    ).build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }
}
