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

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileLookup;
import org.sonar.server.qualityprofile.QProfileName;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.ws.WsTester;
import org.sonar.test.DbTests;

import static org.mockito.Mockito.mock;

@Category(DbTests.class)
public class QProfileInheritanceActionTest {

  @ClassRule
  public static final DbTester dbTester = new DbTester();

  private WsTester wsTester;

  private DbClient dbClient;

  private DbSession session;

  @Before
  public void setUp() throws Exception {
    dbTester.truncateTables();
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(),
      new QualityProfileDao(dbTester.myBatis(), mock(System2.class)));
    session = dbClient.openSession(false);

    wsTester = new WsTester(new QProfilesWs(
      mock(RuleActivationActions.class),
      mock(BulkRuleActivationActions.class),
      mock(ProjectAssociationActions.class),
      new QProfileInheritanceAction(dbClient, new QProfileLookup(dbClient), new QProfileFactory(dbClient), LanguageTesting.newLanguages("xoo"))));
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void inheritance_nominal() throws Exception {
    /*
     * groupWide <- companyWide <- buWide <- (forProject1, forProject2)
     */
    QualityProfileDto groupWide = QProfileTesting.newDto(QProfileName.createFor("xoo", "My Group Profile"), "xoo-my-group-profile-01234");
    QualityProfileDto companyWide = QProfileTesting.newDto(QProfileName.createFor("xoo", "My Company Profile"), "xoo-my-company-profile-12345").setParentKee(groupWide.getKee());
    QualityProfileDto buWide = QProfileTesting.newDto(QProfileName.createFor("xoo", "My BU Profile"), "xoo-my-by-profile-23456").setParentKee(companyWide.getKee());
    QualityProfileDto forProject1 = QProfileTesting.newDto(QProfileName.createFor("xoo", "For Project One"), "xoo-for-project-one-34567").setParentKee(buWide.getKee());
    QualityProfileDto forProject2 = QProfileTesting.newDto(QProfileName.createFor("xoo", "For Project Two"), "xoo-for-project-two-45678").setParentKee(buWide.getKee());

    dbClient.qualityProfileDao().insert(session, groupWide, companyWide, buWide, forProject1, forProject2);
    session.commit();

    wsTester.newGetRequest("api/qualityprofiles", "inheritance").setParam("profileKey", buWide.getKee()).execute().assertJson(getClass(), "inheritance-buWide.json");
  }

  @Test(expected = NotFoundException.class)
  public void fail_if_not_found() throws Exception {
    wsTester.newGetRequest("api/qualityprofiles", "inheritance").setParam("profileKey", "polop").execute();
  }
}
