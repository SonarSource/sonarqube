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
package org.sonar.server.qualityprofile.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Languages;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.server.qualityprofile.QProfileComparison;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

public class CompareActionTest {

  @Rule
  public DbTester dbTester = DbTester.create();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private WsActionTester wsTester;
  private CompareAction underTest;

  @Before
  public void before() {
    underTest = new CompareAction(dbTester.getDbClient(), new QProfileComparison(dbTester.getDbClient()), new Languages());
    wsTester = new WsActionTester(underTest);
  }

  @Test
  public void should_not_allow_to_compare_quality_profiles_from_different_organizations() {
    QProfileDto left = QualityProfileTesting.newQualityProfileDto();
    QProfileDto right = QualityProfileTesting.newQualityProfileDto();
    dbTester.qualityProfiles().insert(left, right);

    TestRequest request = wsTester.newRequest().setMethod("POST")
      .setParam("leftKey", left.getKee())
      .setParam("rightKey", right.getKee());

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Cannot compare quality profiles of different organizations.");
    request.execute();
  }
}
