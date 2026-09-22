/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.v2.api.analysis.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.rule.ActiveRuleRestReponse;
import org.sonar.server.rule.ActiveRuleService;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActiveRulesHandlerImplTest {

  private final DbClient dbClient = mock(DbClient.class, RETURNS_DEEP_STUBS);
  private final DbSession dbSession = mock();
  private final ActiveRuleService activeRuleService = mock(ActiveRuleService.class);
  private final UserSession userSession = mock(UserSession.class);

  @BeforeEach
  void setup() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
  }

  ActiveRulesHandlerImpl underTest = new ActiveRulesHandlerImpl(dbClient, activeRuleService, userSession);

  @Test
  void getActiveRules_returns_default_quality_profile_for_unknown_project_when_caller_has_global_scan_permission() {
    when(userSession.hasPermission(GlobalPermission.SCAN)).thenReturn(true);
    var defaultActiveRule1 = mock(ActiveRuleRestReponse.ActiveRule.class);
    when(activeRuleService.buildDefaultActiveRules()).thenReturn(List.of(defaultActiveRule1));

    List<ActiveRuleRestReponse.ActiveRule> result = underTest.getActiveRules("unknown-project");

    assertThat(result).containsExactly(defaultActiveRule1);
  }

  @Test
  void getActiveRules_returns_default_quality_profile_for_unknown_project_when_caller_has_provision_projects_permission() {
    when(userSession.hasPermission(GlobalPermission.SCAN)).thenReturn(false);
    when(userSession.hasPermission(GlobalPermission.PROVISION_PROJECTS)).thenReturn(true);
    var defaultActiveRule1 = mock(ActiveRuleRestReponse.ActiveRule.class);
    when(activeRuleService.buildDefaultActiveRules()).thenReturn(List.of(defaultActiveRule1));

    List<ActiveRuleRestReponse.ActiveRule> result = underTest.getActiveRules("unknown-project");

    assertThat(result).containsExactly(defaultActiveRule1);
  }

  @Test
  void getActiveRules_rejects_unknown_project_when_caller_has_no_global_scan_nor_provisioning_permission() {
    when(userSession.hasPermission(GlobalPermission.SCAN)).thenReturn(false);
    when(userSession.hasPermission(GlobalPermission.PROVISION_PROJECTS)).thenReturn(false);

    assertThatThrownBy(() -> underTest.getActiveRules("unknown-project")).isInstanceOf(ForbiddenException.class);
    verify(activeRuleService, never()).buildDefaultActiveRules();
  }

  @Test
  void getActiveRules_returns_associated_quality_profile_for_known_project_when_caller_has_scan_permission() {
    ProjectDto projectDto = new ProjectDto().setUuid("someProjectUuid");
    when(dbClient.projectDao().selectProjectByKey(dbSession, "my-project")).thenReturn(Optional.of(projectDto));
    when(userSession.hasEntityPermission(ProjectPermission.SCAN, projectDto)).thenReturn(true);
    var activeRule1 = mock(ActiveRuleRestReponse.ActiveRule.class);
    when(activeRuleService.buildActiveRules("someProjectUuid")).thenReturn(List.of(activeRule1));

    List<ActiveRuleRestReponse.ActiveRule> result = underTest.getActiveRules("my-project");

    assertThat(result).containsExactly(activeRule1);
  }

  @Test
  void getActiveRules_returns_associated_quality_profile_for_known_project_when_caller_has_global_scan_permission() {
    ProjectDto projectDto = new ProjectDto().setUuid("someProjectUuid");
    when(dbClient.projectDao().selectProjectByKey(dbSession, "my-project")).thenReturn(Optional.of(projectDto));
    when(userSession.hasEntityPermission(ProjectPermission.SCAN, projectDto)).thenReturn(false);
    when(userSession.hasPermission(GlobalPermission.SCAN)).thenReturn(true);
    var activeRule1 = mock(ActiveRuleRestReponse.ActiveRule.class);
    when(activeRuleService.buildActiveRules("someProjectUuid")).thenReturn(List.of(activeRule1));

    List<ActiveRuleRestReponse.ActiveRule> result = underTest.getActiveRules("my-project");

    assertThat(result).containsExactly(activeRule1);
  }

  @Test
  void getActiveRules_rejects_known_project_when_caller_has_no_scan_permission_on_it() {
    ProjectDto projectDto = new ProjectDto().setUuid("someProjectUuid");
    when(dbClient.projectDao().selectProjectByKey(dbSession, "private-project")).thenReturn(Optional.of(projectDto));
    when(userSession.hasEntityPermission(ProjectPermission.SCAN, projectDto)).thenReturn(false);
    when(userSession.hasPermission(GlobalPermission.SCAN)).thenReturn(false);

    assertThatThrownBy(() -> underTest.getActiveRules("private-project")).isInstanceOf(ForbiddenException.class);
    verify(activeRuleService, never()).buildActiveRules(any());
    verify(activeRuleService, never()).buildDefaultActiveRules();
  }

}
