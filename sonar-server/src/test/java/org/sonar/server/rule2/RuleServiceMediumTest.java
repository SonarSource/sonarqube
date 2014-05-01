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
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.check.Cardinality;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.search.Hit;
import org.sonar.server.tester.ServerTester;

import java.util.Collection;

import static org.fest.assertions.Assertions.assertThat;

public class RuleServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  @Before
  public void clear_data_store() {
    tester.clearDataStores();
  }

  @Test
  public void insert_in_db_and_index_in_es() {
    // insert db
    RuleKey ruleKey = RuleKey.of("javascript", "S001");
    RuleDao dao = tester.get(RuleDao.class);
    dao.insert(newRuleDto(ruleKey));

    // verify that rule is persisted in db
    RuleDto persistedDto = dao.selectByKey(ruleKey);
    assertThat(persistedDto).isNotNull();
    assertThat(persistedDto.getId()).isGreaterThanOrEqualTo(0);
    assertThat(persistedDto.getRuleKey()).isEqualTo(ruleKey.rule());
    assertThat(persistedDto.getLanguage()).isEqualTo("js");

    // verify that rule is indexed in es
    RuleIndex index = tester.get(RuleIndex.class);
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

  //TODO test delete, update, tags, params

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
