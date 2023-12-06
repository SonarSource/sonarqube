/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.common.rule.service;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.common.rule.RuleCreator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RuleServiceTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DbClient dbClient;

  @Mock
  private RuleCreator ruleCreator;

  @InjectMocks
  private RuleService ruleService;

  @Test
  public void createCustomRule_shouldReturnExceptedRuleInformation() {
    when(ruleCreator.create(Mockito.any(), Mockito.any(NewCustomRule.class))).thenReturn(new RuleDto().setUuid("uuid"));
    when(dbClient.ruleDao().selectRuleParamsByRuleUuids(any(), eq(List.of("uuid")))).thenReturn(List.of(new RuleParamDto().setUuid("paramUuid")));
    RuleInformation customRule = ruleService.createCustomRule(NewCustomRule.createForCustomRule(RuleKey.of("rep", "key"), RuleKey.of("rep", "custom")));
    assertThat(customRule.ruleDto().getUuid()).isEqualTo("uuid");
    assertThat(customRule.params()).extracting(RuleParamDto::getUuid).containsExactly("paramUuid");

    customRule = ruleService.createCustomRule(NewCustomRule.createForCustomRule(RuleKey.of("rep", "key"), RuleKey.of("rep", "custom")), mock(DbSession.class));
    assertThat(customRule.ruleDto().getUuid()).isEqualTo("uuid");
    assertThat(customRule.params()).extracting(RuleParamDto::getUuid).containsExactly("paramUuid");
  }

}
