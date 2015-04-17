/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.qualityprofile.ws;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.QProfileLoader;
import org.sonar.server.qualityprofile.QProfileLookup;
import org.sonar.server.ws.WsTester;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QProfileSearchActionTest {

  @ClassRule
  public static DbTester dbTester = new DbTester();

  private DbClient dbClient;

  private QualityProfileDao qualityProfileDao;

  private Language xoo1, xoo2;

  private WsTester tester;

  private DbSession session;

  private QProfileLoader profileLoader;

  @Before
  public void setUp() throws Exception {
    dbTester.truncateTables();
    qualityProfileDao = new QualityProfileDao(dbTester.myBatis(), mock(System2.class));
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), qualityProfileDao);
    session = dbClient.openSession(false);

    // TODO Replace with actual implementation after removal of DaoV2...
    profileLoader = mock(QProfileLoader.class);

    xoo1 = LanguageTesting.newLanguage("xoo1");
    xoo2 = LanguageTesting.newLanguage("xoo2");

    tester = new WsTester(new QProfilesWs(
      mock(RuleActivationActions.class),
      mock(BulkRuleActivationActions.class),
      mock(ProjectAssociationActions.class),
      new QProfileSearchAction(new Languages(xoo1, xoo2), new QProfileLookup(dbClient), profileLoader, qualityProfileDao)));
  }

  @After
  public void teadDown() {
    session.close();
  }

  @Test
  public void search_nominal() throws Exception {
    when(profileLoader.countAllActiveRules()).thenReturn(ImmutableMap.of(
      "sonar-way-xoo1-12345", 11L,
      "my-sonar-way-xoo2-34567", 33L
      ));

    qualityProfileDao.insert(session,
      QualityProfileDto.createFor("sonar-way-xoo1-12345").setLanguage(xoo1.getKey()).setName("Sonar way").setDefault(true),
      QualityProfileDto.createFor("sonar-way-xoo2-23456").setLanguage(xoo2.getKey()).setName("Sonar way"),
      QualityProfileDto.createFor("my-sonar-way-xoo2-34567").setLanguage(xoo2.getKey()).setName("My Sonar way").setParentKee("sonar-way-xoo2-23456"),
      QualityProfileDto.createFor("sonar-way-other-666").setLanguage("other").setName("Sonar way").setDefault(true)
      );
    new ComponentDao(mock(System2.class)).insert(session,
      ComponentTesting.newProjectDto("project-uuid1"),
      ComponentTesting.newProjectDto("project-uuid2"));
    qualityProfileDao.insertProjectProfileAssociation("project-uuid1", "sonar-way-xoo2-23456", session);
    qualityProfileDao.insertProjectProfileAssociation("project-uuid2", "sonar-way-xoo2-23456", session);
    session.commit();

    tester.newGetRequest("api/qualityprofiles", "search").execute().assertJson(this.getClass(), "search.json");
  }

  @Test
  public void search_with_fields() throws Exception {
    qualityProfileDao.insert(session,
      QualityProfileDto.createFor("sonar-way-xoo1-12345").setLanguage(xoo1.getKey()).setName("Sonar way").setDefault(true),
      QualityProfileDto.createFor("sonar-way-xoo2-23456").setLanguage(xoo2.getKey()).setName("Sonar way"),
      QualityProfileDto.createFor("my-sonar-way-xoo2-34567").setLanguage(xoo2.getKey()).setName("My Sonar way").setParentKee("sonar-way-xoo2-23456")
      );
    session.commit();

    tester.newGetRequest("api/qualityprofiles", "search").setParam("f", "key,language").execute().assertJson(this.getClass(), "search_fields.json");
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_unknown_fields() throws Exception {
    tester.newGetRequest("api/qualityprofiles", "search").setParam("f", "polop").execute();
  }

  @Test
  public void search_for_language() throws Exception {
    qualityProfileDao.insert(session,
      QualityProfileDto.createFor("sonar-way-xoo1-12345").setLanguage(xoo1.getKey()).setName("Sonar way")
      );
    session.commit();

    tester.newGetRequest("api/qualityprofiles", "search").setParam("language", xoo1.getKey()).execute().assertJson(this.getClass(), "search_xoo1.json");
  }
}
