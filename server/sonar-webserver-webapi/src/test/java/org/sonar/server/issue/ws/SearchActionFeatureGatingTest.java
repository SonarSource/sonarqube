/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.server.issue.FromSonarQubeUpdateFeature;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.IssueQueryFactory;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_FROM_SONAR_QUBE_UPDATE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PRIORITIZED_RULE;

class SearchActionFeatureGatingTest {

  private final UserSessionRule userSession = mock(UserSessionRule.class);
  private final IssueIndex issueIndex = mock(IssueIndex.class);
  private final IssueQueryFactory issueQueryFactory = mock(IssueQueryFactory.class);
  private final IssueIndexSyncProgressChecker issueIndexSyncProgressChecker = mock(IssueIndexSyncProgressChecker.class);
  private final SearchResponseLoader searchResponseLoader = mock(SearchResponseLoader.class);
  private final SearchResponseFormat searchResponseFormat = mock(SearchResponseFormat.class);
  private final DbClient dbClient = mock(DbClient.class);

  @Test
  void whenFromSonarQubeUpdateFeatureIsDisabled_parameterShouldNotBeAvailable() {
    FromSonarQubeUpdateFeature fromSonarQubeUpdateFeature = mock(FromSonarQubeUpdateFeature.class);
    when(fromSonarQubeUpdateFeature.isAvailable()).thenReturn(false);

    WsActionTester ws = new WsActionTester(
      new SearchAction(userSession, issueIndex, issueQueryFactory, issueIndexSyncProgressChecker, 
        searchResponseLoader, searchResponseFormat, System2.INSTANCE, dbClient, fromSonarQubeUpdateFeature));

    WebService.Action definition = ws.getDef();
    
    assertThat(definition.param(PARAM_FROM_SONAR_QUBE_UPDATE)).isNull();
    assertThat(definition.param(PARAM_PRIORITIZED_RULE)).isNotNull();
  }

  @Test
  void whenFromSonarQubeUpdateFeatureIsEnabled_parameterShouldBeAvailable() {
    FromSonarQubeUpdateFeature fromSonarQubeUpdateFeature = mock(FromSonarQubeUpdateFeature.class);
    when(fromSonarQubeUpdateFeature.isAvailable()).thenReturn(true);

    WsActionTester ws = new WsActionTester(
      new SearchAction(userSession, issueIndex, issueQueryFactory, issueIndexSyncProgressChecker, 
        searchResponseLoader, searchResponseFormat, System2.INSTANCE, dbClient, fromSonarQubeUpdateFeature));

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

    WsActionTester wsEnabled = new WsActionTester(
      new SearchAction(userSession, issueIndex, issueQueryFactory, issueIndexSyncProgressChecker, 
        searchResponseLoader, searchResponseFormat, System2.INSTANCE, dbClient, enabledFeature));
    
    WsActionTester wsDisabled = new WsActionTester(
      new SearchAction(userSession, issueIndex, issueQueryFactory, issueIndexSyncProgressChecker, 
        searchResponseLoader, searchResponseFormat, System2.INSTANCE, dbClient, disabledFeature));

    assertThat(Objects.requireNonNull(wsEnabled.getDef().param("facets")).possibleValues()).contains(PARAM_FROM_SONAR_QUBE_UPDATE);
    assertThat(Objects.requireNonNull(wsDisabled.getDef().param("facets")).possibleValues()).doesNotContain(PARAM_FROM_SONAR_QUBE_UPDATE);
    
    // PARAM_PRIORITIZED_RULE should always be included
    assertThat(Objects.requireNonNull(wsEnabled.getDef().param("facets")).possibleValues()).contains(PARAM_PRIORITIZED_RULE);
    assertThat(Objects.requireNonNull(wsDisabled.getDef().param("facets")).possibleValues()).contains(PARAM_PRIORITIZED_RULE);
  }
}