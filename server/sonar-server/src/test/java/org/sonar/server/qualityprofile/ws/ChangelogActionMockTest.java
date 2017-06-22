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

import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.resources.Languages;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileChangeQuery;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.ws.WsTester;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.server.qualityprofile.QProfileTesting.XOO_P1_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_SINCE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_TO;

public class ChangelogActionMockTest {

  private static final long A_DATE = 1_500_000_000_000L;

  @Rule
  public DbTester db = DbTester.create();

  private WsTester ws;
  private ChangelogLoader changelogLoader = mock(ChangelogLoader.class);
  private QProfileWsSupport wsSupport = mock(QProfileWsSupport.class);
  private OrganizationDto organization;

  @Before
  public void before() {
    ws = new WsTester(new QProfilesWs(mock(ActivateRulesAction.class),
      new ChangelogAction(changelogLoader, wsSupport, new Languages(), db.getDbClient())));
    organization = db.organizations().insert();
  }

  @Test
  public void changelog_empty() throws Exception {
    when(wsSupport.getProfile(any(DbSession.class), eq(QProfileReference.fromKey(XOO_P1_KEY)))).thenReturn(QProfileTesting.newXooP1(organization));
    when(changelogLoader.load(any(DbSession.class), any(QProfileChangeQuery.class))).thenReturn(new ChangelogLoader.Changelog(0, Collections.emptyList()));

    ws.newGetRequest(QProfilesWs.API_ENDPOINT, "changelog").setParam(PARAM_PROFILE, XOO_P1_KEY)
      .execute().assertJson(getClass(), "changelog_empty.json");
  }

  @Test
  public void changelog_nominal() throws Exception {
    when(wsSupport.getProfile(any(DbSession.class), eq(QProfileReference.fromKey(XOO_P1_KEY)))).thenReturn(QProfileTesting.newXooP1(organization));
    ChangelogLoader.Change change1 = new ChangelogLoader.Change("C1", "ACTIVATED", A_DATE, null, null, null, null, null, null);
    ChangelogLoader.Change change2 = new ChangelogLoader.Change("C2", "ACTIVATED", A_DATE + 10, null, null, null, null, null, null);
    List<ChangelogLoader.Change> changes = asList(change1, change2);
    when(changelogLoader.load(any(DbSession.class), any(QProfileChangeQuery.class))).thenReturn(new ChangelogLoader.Changelog(10, changes));

    ws.newGetRequest(QProfilesWs.API_ENDPOINT, "changelog").setParam(PARAM_PROFILE, XOO_P1_KEY)
      .execute().assertJson(getClass(), "changelog_nominal.json");
  }

  @Test
  public void changelog_with_all_fields() throws Exception {
    when(wsSupport.getProfile(any(DbSession.class), eq(QProfileReference.fromKey(XOO_P1_KEY)))).thenReturn(QProfileTesting.newXooP1(organization));
    ChangelogLoader.Change change1 = new ChangelogLoader.Change("C1", "ACTIVATED", A_DATE, "MAJOR", "marcel", "Marcel", "INHERITED", RuleTesting.XOO_X1, "X One");
    change1.getParams().put("foo", "foo_value");
    change1.getParams().put("bar", "bar_value");
    List<ChangelogLoader.Change> changes = asList(change1);
    when(changelogLoader.load(any(DbSession.class), any(QProfileChangeQuery.class))).thenReturn(new ChangelogLoader.Changelog(10, changes));

    ws.newGetRequest(QProfilesWs.API_ENDPOINT, "changelog").setParam(PARAM_PROFILE, XOO_P1_KEY)
      .execute().assertJson(getClass(), "changelog_full.json");
  }

  @Test
  public void changelog_inclusive_for_dates() throws Exception {
    when(wsSupport.getProfile(any(DbSession.class), eq(QProfileReference.fromKey(XOO_P1_KEY)))).thenReturn(QProfileTesting.newXooP1(organization));
    when(changelogLoader.load(any(DbSession.class), any(QProfileChangeQuery.class))).thenReturn(new ChangelogLoader.Changelog(0, Collections.emptyList()));

    ws.newGetRequest(QProfilesWs.API_ENDPOINT, "changelog")
      .setParam(PARAM_PROFILE, XOO_P1_KEY)
      .setParam(PARAM_SINCE, "2016-09-01")
      .setParam(PARAM_TO, "2016-09-01")
      .execute();

    ArgumentCaptor<QProfileChangeQuery> argumentCaptor = ArgumentCaptor.forClass(QProfileChangeQuery.class);
    verify(changelogLoader).load(any(DbSession.class), argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getFromIncluded()).isEqualTo(parseDate("2016-09-01").getTime());
    assertThat(argumentCaptor.getValue().getToExcluded()).isEqualTo(parseDate("2016-09-02").getTime());
  }

  @Test(expected = NotFoundException.class)
  public void fail_on_unknown_profile() throws Exception {
    when(wsSupport.getProfile(any(DbSession.class), eq(QProfileReference.fromKey(XOO_P1_KEY)))).thenThrow(new NotFoundException("Profile not found"));

    ws.newGetRequest(QProfilesWs.API_ENDPOINT, "changelog").setParam(PARAM_PROFILE, XOO_P1_KEY).execute();
  }
}
