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
package org.sonar.server.rule2;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.check.Cardinality;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.search.Hit;
import org.sonar.server.tester.ServerTester;

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

public class RuleMediumTest {

  @Rule
  public ServerTester tester = new ServerTester()
    // .setProperty("sonar.log.profilingLevel", "FULL")
    .setProperty("sonar.es.http.port", "8888");

  private RuleDto dto;
  private ActiveRuleDto adto;

  @Before
  public void setup() {
    dto = getRuleDto(1);
    adto = getActiveRuleDto(dto);
  }

  @After
  public void teardown() {
    tester.stop();
  }

  private ActiveRuleDto getActiveRuleDto(RuleDto dto) {
    return new ActiveRuleDto()
      .setId(1)
      .setNoteCreatedAt(new Date())
      .setNoteData("Note data, and some more note and here too.")
      .setRuleId(dto.getId())
      .setProfileId(1)
      .setParentId(0)
      .setSeverity(3);
  }

  private RuleDto getRuleDto(int id) {
    return new RuleDto()
      // .setId(id)
      .setRuleKey("NewRuleKey=" + id)
      .setRepositoryKey("plugin")
      .setName("new name")
      .setDescription("new description")
      .setStatus(org.sonar.api.rules.Rule.STATUS_DEPRECATED)
      .setConfigKey("NewConfigKey")
      .setSeverity(Severity.INFO)
      .setCardinality(Cardinality.MULTIPLE)
      .setLanguage("dart")
      .setParentId(3)
      .setSubCharacteristicId(100)
      .setDefaultSubCharacteristicId(101)
      .setRemediationFunction("linear")
      .setDefaultRemediationFunction("linear_offset")
      .setRemediationCoefficient("1h")
      .setDefaultRemediationCoefficient("5d")
      .setRemediationOffset("5min")
      .setDefaultRemediationOffset("10h")
      .setEffortToFixDescription("squid.S115.effortToFix")
      .setCreatedAt(DateUtils.parseDate("2013-12-16"))
      .setUpdatedAt(DateUtils.parseDate("2013-12-17"));
  }

  @Test
  public void test_dao_queue_es_search_loop() {

    RuleDao dao = tester.get(RuleDao.class);
    ActiveRuleDao adao = tester.get(ActiveRuleDao.class);
    RuleIndex index = tester.get(RuleIndex.class);

    dao.insert(dto);

    Hit hit = index.getByKey(dto.getKey());
    assertThat(hit.getFields().get(RuleNormalizer.RuleFields.RULE_KEY.key())).isEqualTo(dto.getRuleKey().toString());
  }

  @Test
  @Ignore
  public void test_ruleservice_getByKey() {
    RuleService service = tester.get(RuleService.class);
    RuleDao dao = tester.get(RuleDao.class);

    dao.insert(dto);

    org.sonar.server.rule2.Rule rule = service.getByKey(dto.getKey());

    assertThat(rule.key()).isEqualTo(dto.getKey());

  }
}
