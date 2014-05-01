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

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Cardinality;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.search.BaseNormalizer;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class RuleNormalizer extends BaseNormalizer<RuleDto, RuleKey> {

  private static final Logger LOG = LoggerFactory.getLogger(RuleNormalizer.class);

  private RuleDao ruleDao;

  public enum RuleField {
    KEY("key"),
    REPOSITORY("repo"),
    NAME("name"),
    CREATED_AT("createdAt"),
    DESCRIPTION("desc"),
    SEVERITY("severity"),
    STATUS("status"),
    LANGUAGE("lang"),
    TAGS("tags"),
    SYSTEM_TAGS("sysTags"),
    INTERNAL_KEY("internalKey"),
    TEMPLATE("template"),
    UPDATED_AT("updatedAt");

    private final String key;

    private RuleField(final String key) {
      this.key = key;
    }

    public String key() {
      return key;
    }

    public String toString() {
      return key;
    }
  }

  public RuleNormalizer(RuleDao ruleDao) {
    this.ruleDao = ruleDao;
  }

  @Override
  public XContentBuilder normalize(RuleKey key) throws IOException {
    return normalize(ruleDao.getByKey(key));
  }

  @Override
  public XContentBuilder normalize(RuleDto rule) throws IOException {
    XContentBuilder document = jsonBuilder().startObject();
    indexField(RuleField.KEY.key(), rule.getRuleKey(), document);
    indexField(RuleField.REPOSITORY.key(), rule.getRepositoryKey(), document);
    indexField(RuleField.NAME.key(), rule.getName(), document);
    indexField(RuleField.CREATED_AT.key(), rule.getCreatedAt(), document);
    indexField(RuleField.UPDATED_AT.key(), rule.getUpdatedAt(), document);
    indexField(RuleField.DESCRIPTION.key(), rule.getDescription(), document);
    indexField(RuleField.SEVERITY.key(), rule.getSeverityString(), document);
    indexField(RuleField.STATUS.key(), rule.getStatus(), document);
    indexField(RuleField.LANGUAGE.key(), rule.getLanguage(), document);
    indexField(RuleField.INTERNAL_KEY.key(), rule.getConfigKey(), document);
    indexField(RuleField.TEMPLATE.key(), rule.getCardinality() == Cardinality.MULTIPLE, document);
    indexField(RuleField.TAGS.key(), rule.getName(), document);
    indexField(RuleField.SYSTEM_TAGS.key(), rule.getName(), document);

    // document.startArray("active");
    // for (ActiveRuleDto activeRule : activeRuleDao.selectByRuleId(rule.getId())) {
    // document.startObject();
    // Map<String, Object> activeRuleProperties = BeanUtils.describe(activeRule);
    // for (Entry<String, Object> activeRuleProp : activeRuleProperties.entrySet()) {
    // LOG.trace("NORMALIZING: --- {} -> {}", activeRuleProp.getKey(), activeRuleProp.getValue());
    // document.field(activeRuleProp.getKey(), activeRuleProp.getValue());
    // }
    // document.endObject();
    // }
    // document.endArray();

    return document.endObject();
  }

}
