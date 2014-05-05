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
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.core.rule.RuleRuleTagDto;
import org.sonar.core.rule.RuleTagDao;
import org.sonar.core.rule.RuleTagDto;
import org.sonar.core.rule.RuleTagType;
import org.sonar.server.search.Hit;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.tester.ServerTester;

import java.util.List;
import java.util.Map;

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
    assertThat((Boolean) hit.getField(RuleNormalizer.RuleField.TEMPLATE.key())).isFalse();

    //TODO    assertThat((Collection) hit.getField(RuleNormalizer.RuleField.SYSTEM_TAGS.key())).isEmpty();
    //TODO    assertThat((Collection) hit.getField(RuleNormalizer.RuleField.TAGS.key())).isEmpty();
  }

  @Test
  public void insert_and_index_rule_parameters() {
    DbSession dbSession = tester.get(MyBatis.class).openSession(false);

    // insert db
    RuleKey ruleKey = RuleKey.of("javascript", "S001");
    RuleDto ruleDto = newRuleDto(ruleKey);
    dao.insert(ruleDto, dbSession);
    dbSession.commit();

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

  @Test
  public void insert_and_index_activeRuleParams() {
    DbSession dbSession = tester.get(MyBatis.class).openSession(false);
    ActiveRuleDao activeRuleDao = tester.get(ActiveRuleDao.class);
    // insert db
    RuleKey ruleKey = RuleKey.of("javascript", "S001");
    RuleDto ruleDto = newRuleDto(ruleKey);
    dao.insert(ruleDto, dbSession);

    RuleParamDto minParam = new RuleParamDto()
      .setRuleId(ruleDto.getId())
      .setName("min")
      .setType("STRING");
    dao.insert(minParam, dbSession);

    RuleParamDto maxParam = new RuleParamDto()
      .setRuleId(ruleDto.getId())
      .setName("max")
      .setType("STRING");
    dao.insert(maxParam, dbSession);


    ActiveRuleDto activeRule = new ActiveRuleDto()
      .setInheritance("inherited")
      .setProfileId(1)
      .setRuleId(ruleDto.getId())
      .setSeverity(Severity.BLOCKER);
    activeRuleDao.insert(activeRule, dbSession);

    ActiveRuleParamDto activeRuleMinParam = new ActiveRuleParamDto()
      .setActiveRuleId(activeRule.getId())
      .setKey(minParam.getName())
      .setValue("minimum")
      .setRulesParameterId(minParam.getId());
    activeRuleDao.insert(activeRuleMinParam, dbSession);

    ActiveRuleParamDto activeRuleMaxParam = new ActiveRuleParamDto()
      .setActiveRuleId(activeRule.getId())
      .setKey(maxParam.getName())
      .setValue("maximum")
      .setRulesParameterId(maxParam.getId());
    activeRuleDao.insert(activeRuleMaxParam, dbSession);

    dbSession.commit();

    // verify that activeRulesParams are persisted in db
    List<ActiveRuleParamDto> persistedDtos = activeRuleDao.selectParamsByActiveRuleId(activeRule.getId());
    assertThat(persistedDtos).hasSize(2);

    // verify that activeRulesParams are indexed in es
    index.refresh();
    Hit hit = index.getByKey(ruleKey);
    assertThat(hit).isNotNull();

    index.search(new RuleQuery(), new QueryOptions());

    Map<String, Map> _activeRules = (Map<String, Map>) hit.getField(RuleNormalizer.RuleField.ACTIVE.key());
    assertThat(_activeRules).isNotNull().hasSize(1);

    Map<String, Object> _activeRule = (Map<String, Object>) Iterables.getFirst(_activeRules.values(),null);
    assertThat(_activeRule.get(RuleNormalizer.RuleField.SEVERITY.key())).isEqualTo(Severity.BLOCKER);

    Map<String, Map> _activeRuleParams = (Map<String, Map>) _activeRule.get(RuleNormalizer.RuleField.PARAMS.key());
    assertThat(_activeRuleParams).isNotNull().hasSize(2);

    Map<String, String> _activeRuleParamValue = (Map<String, String>) _activeRuleParams.get(maxParam.getName());
    assertThat(_activeRuleParamValue).isNotNull().hasSize(1);
    assertThat(_activeRuleParamValue.get(RuleNormalizer.ActiveRuleParamField.VALUE.key())).isEqualTo("maximum");

  }

  //TODO test delete, update, tags, params

  @Test
  public void insert_and_index_tags() {
    DbSession dbSession = tester.get(MyBatis.class).openSession(false);
    RuleTagDao ruleTagDao = tester.get(RuleTagDao.class);

    // insert db
    RuleKey ruleKey = RuleKey.of("javascript", "S001");
    RuleDto ruleDto = newRuleDto(ruleKey);
    dao.insert(ruleDto, dbSession);
    dbSession.commit();

    RuleTagDto tag1 = new RuleTagDto()
      .setTag("hello");
    RuleTagDto tag2 = new RuleTagDto()
      .setTag("world");
    RuleTagDto tag3 = new RuleTagDto()
      .setTag("AdMiN");
    ruleTagDao.insert(tag1, dbSession);
    ruleTagDao.insert(tag2, dbSession);
    ruleTagDao.insert(tag3, dbSession);
    dbSession.commit();

    RuleRuleTagDto rTag1 = new RuleRuleTagDto()
      .setTagId(tag1.getId())
      .setTag(tag1.getTag())
      .setRuleId(ruleDto.getId())
      .setType(RuleTagType.ADMIN);
    RuleRuleTagDto rTag2 = new RuleRuleTagDto()
      .setTagId(tag2.getId())
      .setTag(tag2.getTag())
      .setRuleId(ruleDto.getId())
      .setType(RuleTagType.ADMIN);
    RuleRuleTagDto rTag3 = new RuleRuleTagDto()
      .setTagId(tag3.getId())
      .setTag(tag3.getTag())
      .setRuleId(ruleDto.getId())
      .setType(RuleTagType.SYSTEM);
    dao.insert(rTag1, dbSession);
    dao.insert(rTag2, dbSession);
    dao.insert(rTag3, dbSession);
    dbSession.commit();

    // verify that tags are persisted in db
    List<RuleRuleTagDto> persistedDtos = dao.selectTagsByRuleId(ruleDto.getId());
    assertThat(persistedDtos).hasSize(3);


    // verify that tags are indexed in es
    index.refresh();

    index.search(new RuleQuery(), new QueryOptions());
    Hit hit = index.getByKey(ruleKey);
    assertThat(hit).isNotNull();
    assertThat(hit.getField(RuleNormalizer.RuleField.TAGS.key())).isNotNull();

    RuleService service = tester.get(RuleService.class);
    Rule rule = service.getByKey(ruleKey);
    assertThat(rule.tags()).containsExactly("hello","world");
    assertThat(rule.systemTags()).containsExactly("AdMiN");
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
