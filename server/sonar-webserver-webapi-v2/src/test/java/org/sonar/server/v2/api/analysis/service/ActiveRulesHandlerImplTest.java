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
package org.sonar.server.v2.api.analysis.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.rule.ActiveRuleRestReponse;
import org.sonar.server.rule.ActiveRuleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActiveRulesHandlerImplTest {

  private final DbClient dbClient = mock(DbClient.class, RETURNS_DEEP_STUBS);
  private final DbSession dbSession = mock();
  private final ActiveRuleService activeRuleService = mock(ActiveRuleService.class);

  @BeforeEach
  void setup() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
  }

  ActiveRulesHandlerImpl underTest = new ActiveRulesHandlerImpl(dbClient, activeRuleService);

  @Test
  void getActiveRules_returns_default_quality_profile_for_unknown_project() {
    var defaultActiveRule1 = mock(ActiveRuleRestReponse.ActiveRule.class);
    when(activeRuleService.buildDefaultActiveRules()).thenReturn(List.of(defaultActiveRule1));

    List<ActiveRuleRestReponse.ActiveRule> result = underTest.getActiveRules("unknown-project");

    assertThat(result).containsExactly(defaultActiveRule1);
  }

  @Test
  void getActiveRules_returns_associated_quality_profile_for_known_project() {
    when(dbClient.projectDao().selectProjectByKey(dbSession, "my-project")).thenReturn(Optional.of(new ProjectDto().setUuid("someProjectUuid")));
    var activeRule1 = mock(ActiveRuleRestReponse.ActiveRule.class);
    when(activeRuleService.buildActiveRules("someProjectUuid")).thenReturn(List.of(activeRule1));

    List<ActiveRuleRestReponse.ActiveRule> result = underTest.getActiveRules("my-project");

    assertThat(result).containsExactly(activeRule1);
  }

}
