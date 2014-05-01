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

import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.utils.DateUtils;
import org.sonar.check.Cardinality;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.search.Hit;
import org.sonar.server.tester.ServerTester;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class RuleServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  RuleDao dao = tester.get(RuleDao.class);
  RuleIndex index = tester.get(RuleIndex.class);

  @Before
  public void clear_data_store() {
    tester.clearDataStores();
  }

  @Test
  public void insert_in_db_and_index_in_es() {
    // insert db
    RuleKey ruleKey = RuleKey.of("javascript", "S001");
    dao.insert(newRuleDto(ruleKey));

    // verify that rule is persisted in db
    RuleDto persistedDto = dao.selectByKey(ruleKey);
    assertThat(persistedDto).isNotNull();
    assertThat(persistedDto.getId()).isGreaterThanOrEqualTo(0);
    assertThat(persistedDto.getRuleKey()).isEqualTo(ruleKey.rule());
    assertThat(persistedDto.getLanguage()).isEqualTo("js");

    // verify that rule is indexed in es
    index.refresh();
    Hit hit = index.getByKey(ruleKey);
    assertThat(hit).isNotNull();
    assertThat(hit.getFieldAsString(RuleNormalizer.RuleField.REPOSITORY.key())).isEqualTo(ruleKey.repository());
    assertThat(hit.getFieldAsString(RuleNormalizer.RuleField.KEY.key())).isEqualTo(ruleKey.rule());
    assertThat(hit.getFieldAsString(RuleNormalizer.RuleField.LANGUAGE.key())).isEqualTo("js");
    assertThat(hit.getFieldAsString(RuleNormalizer.RuleField.NAME.key())).isEqualTo("Rule S001");
    assertThat(hit.getFieldAsString(RuleNormalizer.RuleField.DESCRIPTION.key())).isEqualTo("Description S001");
    assertThat(hit.getFieldAsString(RuleNormalizer.RuleField.STATUS.key())).isEqualTo(RuleStatus.READY.toString());
    assertThat(hit.getField(RuleNormalizer.RuleField.CREATED_AT.key())).isNotNull();
    assertThat(hit.getField(RuleNormalizer.RuleField.UPDATED_AT.key())).isNotNull();
    assertThat(hit.getFieldAsString(RuleNormalizer.RuleField.INTERNAL_KEY.key())).isEqualTo("InternalKeyS001");
    assertThat(hit.getFieldAsString(RuleNormalizer.RuleField.SEVERITY.key())).isEqualTo("INFO");
//TODO    assertThat((Collection) hit.getField(RuleNormalizer.RuleField.SYSTEM_TAGS.key())).isEmpty();
//TODO    assertThat((Collection) hit.getField(RuleNormalizer.RuleField.TAGS.key())).isEmpty();
    assertThat((Boolean) hit.getField(RuleNormalizer.RuleField.TEMPLATE.key())).isFalse();
  }

  @Test
  public void insert_and_index_rule_parameters() {
    DbSession dbSession = tester.get(MyBatis.class).openSession(false);

    // insert db
    RuleKey ruleKey = RuleKey.of("javascript", "S001");
    RuleDto ruleDto = newRuleDto(ruleKey);
    dao.insert(ruleDto, dbSession);
    RuleParamDto minParamDto = new RuleParamDto()
      .setRuleId(ruleDto.getId())
      .setName("min")
      .setType(RuleParamType.INTEGER.type())
      .setDefaultValue("2")
      .setDescription("Minimum");
    dao.insert(minParamDto, dbSession);
    RuleParamDto maxParamDto = new RuleParamDto()
      .setRuleId(ruleDto.getId())
      .setName("max")
      .setType(RuleParamType.INTEGER.type())
      .setDefaultValue("10")
      .setDescription("Maximum");
    dao.insert(maxParamDto, dbSession);
    dbSession.commit();

    // verify that parameters are persisted in db
    List<RuleParamDto> persistedDtos = dao.selectParametersByRuleId(ruleDto.getId());
    assertThat(persistedDtos).hasSize(2);

    // verify that parameters are indexed in es
    index.refresh();
    Hit hit = index.getByKey(ruleKey);
    assertThat(hit).isNotNull();
    assertThat(hit.getField(RuleNormalizer.RuleField.PARAMS.key())).isNotNull();


    RuleService service = tester.get(RuleService.class);
    Rule rule = service.getByKey(ruleKey);

    assertThat(rule.params()).hasSize(2);
    assertThat(Iterables.getLast(rule.params(), null).key()).isEqualTo("max");
  }

  //TODO test delete, update, tags, params

  @Test
  public void insert_and_index_activeRules() {
    DbSession dbSession = tester.get(MyBatis.class).openSession(false);
    ActiveRuleDao activeRuleDao = tester.get(ActiveRuleDao.class);
    // insert db
    RuleKey ruleKey = RuleKey.of("javascript", "S001");
    RuleDto ruleDto = newRuleDto(ruleKey);
    dao.insert(ruleDto, dbSession);
    ActiveRuleDto activeRule = new ActiveRuleDto()
      .setInheritance("inherited")
      .setProfileId(1)
      .setRuleId(ruleDto.getId())
      .setSeverity(Severity.BLOCKER);
    activeRuleDao.insert(activeRule, dbSession);
    dbSession.commit();

    // verify that activeRules are persisted in db
    List<ActiveRuleDto> persistedDtos = activeRuleDao.selectByRuleId(ruleDto.getId());
    assertThat(persistedDtos).hasSize(1);

    // verify that activeRules are indexed in es
    index.refresh();
    Hit hit = index.getByKey(ruleKey);
    assertThat(hit).isNotNull();
    assertThat(hit.getField(RuleNormalizer.RuleField.ACTIVE.key())).isNotNull();


    RuleService service = tester.get(RuleService.class);
    Rule rule = service.getByKey(ruleKey);
  }


  private RuleDto newRuleDto(RuleKey ruleKey) {
    return new RuleDto()
      .setRuleKey(ruleKey.rule())
      .setRepositoryKey(ruleKey.repository())
      .setName("Rule " + ruleKey.rule())
      .setDescription("Description " + ruleKey.rule())
      .setStatus(RuleStatus.READY.toString())
      .setConfigKey("InternalKey" + ruleKey.rule())
      .setSeverity(Severity.INFO)
      .setCardinality(Cardinality.SINGLE)
      .setLanguage("js")
      .setRemediationFunction("linear")
      .setDefaultRemediationFunction("linear_offset")
      .setRemediationCoefficient("1h")
      .setDefaultRemediationCoefficient("5d")
      .setRemediationOffset("5min")
      .setDefaultRemediationOffset("10h")
      .setEffortToFixDescription(ruleKey.repository() + "." + ruleKey.rule() + ".effortToFix")
      .setCreatedAt(DateUtils.parseDate("2013-12-16"))
      .setUpdatedAt(DateUtils.parseDate("2013-12-17"));
  }
}
