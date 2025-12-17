/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.issue.ws;

import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.server.feature.JiraSonarQubeFeature;
import org.sonar.server.issue.FromSonarQubeUpdateFeature;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.IssueQueryFactory;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarsource.compliancereports.reports.MetadataLoader;
import org.sonarsource.compliancereports.reports.MetadataRules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_FROM_SONAR_QUBE_UPDATE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_LINKED_TICKET_STATUS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PRIORITIZED_RULE;

class SearchActionFeatureGatingTest {

  private final UserSessionRule userSession = mock(UserSessionRule.class);
  private final IssueIndex issueIndex = mock(IssueIndex.class);
  private final IssueQueryFactory issueQueryFactory = mock(IssueQueryFactory.class);
  private final IssueIndexSyncProgressChecker issueIndexSyncProgressChecker = mock(IssueIndexSyncProgressChecker.class);
  private final SearchResponseLoader searchResponseLoader = mock(SearchResponseLoader.class);
  private final SearchResponseFormat searchResponseFormat = mock(SearchResponseFormat.class);
  private final FromSonarQubeUpdateFeature fromSonarQubeUpdateFeature = mock(FromSonarQubeUpdateFeature.class);
  private final JiraSonarQubeFeature jiraSonarQubeFeature = mock(JiraSonarQubeFeature.class);
  private final DbClient dbClient = mock(DbClient.class);
  private final MetadataLoader metadataLoader = mock(MetadataLoader.class);

  @Test
  void whenFromSonarQubeUpdateFeatureIsDisabled_parameterShouldNotBeAvailable() {
    FromSonarQubeUpdateFeature originFromSonarQubeUpdateFeature = mock(FromSonarQubeUpdateFeature.class);
    when(originFromSonarQubeUpdateFeature.isAvailable()).thenReturn(false);

    WsActionTester ws = createSearchActionTester(originFromSonarQubeUpdateFeature, jiraSonarQubeFeature);

    WebService.Action definition = ws.getDef();

    assertThat(definition.param(PARAM_FROM_SONAR_QUBE_UPDATE)).isNull();
    assertThat(definition.param(PARAM_PRIORITIZED_RULE)).isNotNull();
  }

  @Test
  void whenFromSonarQubeUpdateFeatureIsEnabled_parameterShouldBeAvailable() {
    FromSonarQubeUpdateFeature originFromSonarQubeUpdateFeature = mock(FromSonarQubeUpdateFeature.class);
    when(originFromSonarQubeUpdateFeature.isAvailable()).thenReturn(true);

    WsActionTester ws = createSearchActionTester(originFromSonarQubeUpdateFeature, jiraSonarQubeFeature);

    WebService.Action definition = ws.getDef();

    assertThat(definition.param(PARAM_FROM_SONAR_QUBE_UPDATE)).isNotNull();
    assertThat(definition.param(PARAM_PRIORITIZED_RULE)).isNotNull();
  }

  @Test
  void facetsListShouldIncludeFromSonarQubeUpdateOnlyWhenFeatureEnabled() {
    FromSonarQubeUpdateFeature enabledFeature = mock(FromSonarQubeUpdateFeature.class);
    when(enabledFeature.isAvailable()).thenReturn(true);

    FromSonarQubeUpdateFeature disabledFeature = mock(FromSonarQubeUpdateFeature.class);
    when(disabledFeature.isAvailable()).thenReturn(false);

    WsActionTester wsEnabled = createSearchActionTester(enabledFeature, jiraSonarQubeFeature);
    WsActionTester wsDisabled = createSearchActionTester(disabledFeature, jiraSonarQubeFeature);

    assertThat(Objects.requireNonNull(wsEnabled.getDef().param("facets")).possibleValues()).contains(PARAM_FROM_SONAR_QUBE_UPDATE);
    assertThat(Objects.requireNonNull(wsDisabled.getDef().param("facets")).possibleValues()).doesNotContain(PARAM_FROM_SONAR_QUBE_UPDATE);

    // PARAM_PRIORITIZED_RULE should always be included
    assertThat(Objects.requireNonNull(wsEnabled.getDef().param("facets")).possibleValues()).contains(PARAM_PRIORITIZED_RULE);
    assertThat(Objects.requireNonNull(wsDisabled.getDef().param("facets")).possibleValues()).contains(PARAM_PRIORITIZED_RULE);
  }

  @Test
  void whenJiraFeatureIsDisabled_linkedTicketStatusParameterShouldNotBeAvailable() {
    var disabledJiraSonarQubeFeature = mock(JiraSonarQubeFeature.class);
    when(disabledJiraSonarQubeFeature.isAvailable()).thenReturn(false);
    var ws = createSearchActionTester(fromSonarQubeUpdateFeature, disabledJiraSonarQubeFeature);

    var definition = ws.getDef();

    assertThat(definition).isNotNull();
    assertThat(definition.param(PARAM_LINKED_TICKET_STATUS)).isNull();
  }

  @Test
  void whenJiraFeatureIsEnabled_linkedTicketStatusParameterShouldBeAvailable() {
    var enabledJiraSonarQubeFeature = mock(JiraSonarQubeFeature.class);
    when(enabledJiraSonarQubeFeature.isAvailable()).thenReturn(true);
    var ws = createSearchActionTester(fromSonarQubeUpdateFeature, enabledJiraSonarQubeFeature);

    var definition = ws.getDef();

    assertThat(definition).isNotNull();
    assertThat(definition.param(PARAM_LINKED_TICKET_STATUS)).isNotNull();
  }

  @Test
  void whenJiraFeatureIsEnabled_linkedTicketStatusFacetShouldBeAvailable() {
    var enabledJiraSonarQubeFeature = mock(JiraSonarQubeFeature.class);
    when(enabledJiraSonarQubeFeature.isAvailable()).thenReturn(true);

    var searchActionTester = createSearchActionTester(fromSonarQubeUpdateFeature, enabledJiraSonarQubeFeature);

    assertThat(Objects.requireNonNull(searchActionTester.getDef().param("facets")).possibleValues()).contains(PARAM_LINKED_TICKET_STATUS);
  }

  @Test
  void whenJiraFeatureIsDisabled_linkedTicketStatusFacetShouldNotBeAvailable() {
    var disabledJiraSonarQubeFeature = mock(JiraSonarQubeFeature.class);
    when(disabledJiraSonarQubeFeature.isAvailable()).thenReturn(false);

    var searchActionTester = createSearchActionTester(fromSonarQubeUpdateFeature, disabledJiraSonarQubeFeature);

    assertThat(Objects.requireNonNull(searchActionTester.getDef().param("facets")).possibleValues()).doesNotContain(PARAM_LINKED_TICKET_STATUS);
  }

  private WsActionTester createSearchActionTester(FromSonarQubeUpdateFeature fromSonarQubeUpdateFeature, JiraSonarQubeFeature jiraSonarQubeFeature) {
    return new WsActionTester(
      new SearchAction(
        userSession,
        issueIndex,
        issueQueryFactory,
        issueIndexSyncProgressChecker,
        searchResponseLoader,
        searchResponseFormat,
        System2.INSTANCE,
        dbClient,
        fromSonarQubeUpdateFeature,
        jiraSonarQubeFeature,
        metadataLoader,
        new MetadataRules(metadataLoader)
      )
    );
  }

}
