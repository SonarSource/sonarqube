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
package org.sonar.server.rule.registration;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.DeprecatedRuleKeyDto;
import org.sonar.db.rule.RuleDao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RulesRegistrationContextTest {

  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  void whenDeprecatedRuleIsNotFound_thenWarningLogTraceIsProduced() {
    DbClient dbClient = mock();
    DbSession dbSession = mock();
    RuleDao ruleDao = mock();
    when(dbClient.ruleDao()).thenReturn(ruleDao);
    when(ruleDao.selectAll(dbSession)).thenReturn(List.of());
    when(ruleDao.selectAllDeprecatedRuleKeys(dbSession)).thenReturn(Set.of(
      createDeprecatedRuleKeyDto("oldKey", "oldRepo", "newKey", "uuid")
    ));
    when(ruleDao.selectAllRuleParams(dbSession)).thenReturn(List.of());

    RulesRegistrationContext.create(dbClient, dbSession);

    assertThat(logTester.logs(Level.WARN)).
      contains("Could not retrieve rule with uuid newKey referenced by a deprecated rule key. " +
        "The following deprecated rule keys seem to be referencing a non-existing rule: [oldRepo:oldKey]");
  }

  private DeprecatedRuleKeyDto createDeprecatedRuleKeyDto(String oldKey, String oldRepo, String newKey, String uuid) {
    DeprecatedRuleKeyDto dto = new DeprecatedRuleKeyDto();
    dto.setOldRuleKey(oldKey);
    dto.setOldRepositoryKey(oldRepo);
    dto.setRuleUuid(newKey);
    dto.setUuid(uuid);
    return dto;
  }
}
