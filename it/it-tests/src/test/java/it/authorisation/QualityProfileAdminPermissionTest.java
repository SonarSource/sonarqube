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
package it.authorisation;

import com.sonar.orchestrator.Orchestrator;
import it.Category1Suite;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.client.permission.AddUserWsRequest;
import org.sonarqube.ws.client.permission.PermissionsService;
import util.user.UserRule;

import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.runProjectAnalysis;
import static util.selenium.Selenese.runSelenese;

/**
 * SONAR-4210
 */
public class QualityProfileAdminPermissionTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @ClassRule
  public static UserRule userRule = UserRule.from(orchestrator);

  private static PermissionsService permissionsWsClient;

  @BeforeClass
  public static void init() {
    orchestrator.resetData();
    permissionsWsClient = newAdminWsClient(orchestrator).permissions();
    runProjectAnalysis(orchestrator, "shared/xoo-sample");
  }

  @AfterClass
  public static void clearUsers() throws Exception {
    userRule.resetUsers();
  }

  @Test
  public void permission_should_grant_access_to_profile() {
    userRule.createUser("not_profileadm", "userpwd");
    userRule.createUser("profileadm", "papwd");
    permissionsWsClient.addUser(new AddUserWsRequest().setLogin("profileadm").setPermission("profileadmin"));

    runSelenese(orchestrator,
      // Verify normal user is not allowed to do any modification
      "/authorisation/QualityProfileAdminPermissionTest/normal-user.html",
      // Verify profile admin is allowed to do modifications
      "/authorisation/QualityProfileAdminPermissionTest/profile-admin.html");
  }

}
