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
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.core.rule.RuleRuleTagDto;
import org.sonar.core.rule.RuleTagDao;
import org.sonar.core.rule.RuleTagDto;
import org.sonar.core.rule.RuleTagType;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.tester.ServerTester;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class RuleServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  RuleDao dao = tester.get(RuleDao.class);

  QualityProfileDao qualityProfileDao = tester.get(QualityProfileDao.class);

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
    Rule hit = index.getByKey(ruleKey);
    assertThat(hit).isNotNull();
    assertThat(hit.key().repository()).isEqualTo(ruleKey.repository());
    assertThat(hit.key().rule()).isEqualTo(ruleKey.rule());
    assertThat(hit.language()).isEqualTo("js");
    assertThat(hit.name()).isEqualTo("Rule S001");
    assertThat(hit.htmlDescription()).isEqualTo("Description S001");
    assertThat(hit.status()).isEqualTo(RuleStatus.READY);
    //TODO fix date in ES
//    assertThat(hit.createdAt()).isNotNull();
//    assertThat(hit.updatedAt()).isNotNull();
    assertThat(hit.internalKey()).isEqualTo("InternalKeyS001");
    assertThat(hit.severity()).isEqualTo("INFO");
    assertThat(hit.template()).isFalse();

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
    Rule hit = index.getByKey(ruleKey);
    assertThat(hit).isNotNull();
    assertThat(hit.key()).isNotNull();


    RuleService service = tester.get(RuleService.class);
    Rule rule = service.getByKey(ruleKey);

    assertThat(rule.params()).hasSize(2);
    assertThat(Iterables.getLast(rule.params(), null).key()).isEqualTo("max");
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
    dbSession.close();


    // verify that tags are persisted in db
    List<RuleRuleTagDto> persistedDtos = dao.selectTagsByRuleId(ruleDto.getId());
    assertThat(persistedDtos).hasSize(3);

    index.refresh();

    // verify that tags are indexed in es
    index.search(new RuleQuery(), new QueryOptions());
    Rule hit = index.getByKey(ruleKey);
    assertThat(hit).isNotNull();
    assertThat(hit.tags()).containsExactly("hello","world");
    assertThat(hit.systemTags()).containsExactly("AdMiN");

    RuleService service = tester.get(RuleService.class);
    Rule rule = service.getByKey(ruleKey);
    assertThat(rule.tags()).containsExactly("hello","world");
    assertThat(rule.systemTags()).containsExactly("AdMiN");

    //TODO assertThat(service.listTags()).contains("hello", "world", "AdMiN");
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
