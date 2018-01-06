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
package org.sonarqube.tests.authorization;

import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.client.permissions.AddUserRequest;
import org.sonarqube.ws.client.qualityprofiles.CreateRequest;
import util.selenium.Selenese;

import static util.ItUtils.runProjectAnalysis;

/**
 * SONAR-4210
 */
public class QualityProfileAdminPermissionTest {

  @ClassRule
  public static Orchestrator orchestrator = AuthorizationSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator)
    // all the tests of AuthorizationSuite must disable organizations
    .disableOrganizations();

  @Test
  public void permission_should_grant_access_to_profile() {
    runProjectAnalysis(orchestrator, "shared/xoo-sample");

    tester.users().generate(u -> u.setLogin("not_profileadm").setPassword("userpwd"));
    tester.users().generate(u -> u.setLogin("profileadm").setPassword("papwd"));
    tester.wsClient().permissions().addUser(new AddUserRequest().setLogin("profileadm").setPermission("profileadmin"));
    createProfile("xoo", "foo");

    Selenese.runSelenese(orchestrator,
      // Verify normal user is not allowed to do any modification
      "/authorisation/QualityProfileAdminPermissionTest/normal-user.html",
      // Verify profile admin is allowed to do modifications
      "/authorisation/QualityProfileAdminPermissionTest/profile-admin.html");
  }

  private void createProfile(String language, String name) {
    tester.wsClient().qualityprofiles().create(new CreateRequest()
      .setLanguage(language)
      .setName(name));
  }

}
