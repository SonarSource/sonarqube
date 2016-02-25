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
package org.sonar.server.qualityprofile.ws;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualityprofile.QProfileExporters;
import org.sonar.server.qualityprofile.QProfileExportersTest.StandardExporter;
import org.sonar.server.qualityprofile.QProfileExportersTest.XooExporter;
import org.sonar.server.qualityprofile.QProfileExportersTest.XooProfileDefinition;
import org.sonar.server.qualityprofile.QProfileExportersTest.XooProfileImporter;
import org.sonar.server.qualityprofile.QProfileExportersTest.XooProfileImporterWithError;
import org.sonar.server.qualityprofile.QProfileExportersTest.XooProfileImporterWithMessages;
import org.sonar.server.qualityprofile.QProfileExportersTest.XooRulesDefinition;
import org.sonar.server.qualityprofile.QProfileLoader;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

public class CreateActionMediumTest {

  // TODO Replace with simpler test with DbTester / EsTester after removal of DaoV2
  @ClassRule
  public static ServerTester tester = new ServerTester()
    .withEsIndexes()
    .withStartupTasks()
    .addXoo()
    .addComponents(
      XooRulesDefinition.class, XooProfileDefinition.class,
      XooExporter.class, StandardExporter.class,
      XooProfileImporter.class, XooProfileImporterWithMessages.class, XooProfileImporterWithError.class);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  DbClient db;
  DbSession dbSession;
  QProfileExporters exporters;
  QProfileLoader loader;
  WsTester wsTester;

  @Before
  public void before() {
    db = tester.get(DbClient.class);
    dbSession = db.openSession(false);
    exporters = tester.get(QProfileExporters.class);
    loader = tester.get(QProfileLoader.class);
    wsTester = new WsTester(tester.get(QProfilesWs.class));
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void create_nominal() throws Exception {
    userSessionRule.login("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    wsTester.newPostRequest("api/qualityprofiles", "create")
      .setParam("backup_XooProfileImporter", "a value for xoo importer")
      .setParam("language", "xoo")
      .setParam("name", "My New Profile")
      .execute().assertJson(this.getClass(), "create-nominal.json");
  }

  @Test
  public void create_with_messages() throws Exception {
    userSessionRule.login("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    wsTester.newPostRequest("api/qualityprofiles", "create")
      .setParam("backup_XooProfileImporter", "a value for xoo importer")
      .setParam("backup_XooProfileImporterWithMessages", "this will generate some messages")
      .setParam("language", "xoo")
      .setParam("name", "My Other Profile")
      .execute().assertJson(this.getClass(), "create-with-messages.json");
  }

  @Test(expected = BadRequestException.class)
  public void fail_on_error_from_importer() throws Exception {
    userSessionRule.login("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    wsTester.newPostRequest("api/qualityprofiles", "create")
      .setParam("backup_XooProfileImporter", "a value for xoo importer")
      .setParam("backup_XooProfileImporterWithError", "this will fail")
      .setParam("language", "xoo")
      .setParam("name", "Error In Importer")
      .execute();
  }
}
