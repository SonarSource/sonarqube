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

import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.server.search.BaseNormalizer;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class ActiveRuleNormalizer extends BaseNormalizer<ActiveRuleDto, ActiveRuleKey> {

  ActiveRuleDao activeRuleDao;

  public static enum ActiveRuleField {
    OVERRIDE("override"),
    INHERITANCE("inheritance"),
    PROFILE_ID("profile"),
    SEVERITY("severity"),
    PARENT_ID("parent"),
    PARAMS("params");

    private final String key;

    private ActiveRuleField(final String key) {
      this.key = key;
    }

    public String key() {
      return key;
    }

    @Override
    public String toString() {
      return key;
    }
  }

  public static enum ActiveRuleParamField {
    NAME("name"),
    VALUE("value");

    private final String key;

    private ActiveRuleParamField(final String key) {
      this.key = key;
    }

    public String key() {
      return key;
    }

    @Override
    public String toString() {
      return key;
    }
  }

  public ActiveRuleNormalizer(ActiveRuleDao activeRuleDao) {
    this.activeRuleDao = activeRuleDao;
  }

  @Override
  public UpdateRequest normalize(ActiveRuleKey key)  throws Exception {
    return normalize(activeRuleDao.getByKey(key));
  }

  public UpdateRequest normalize(ActiveRuleParamDto param, ActiveRuleKey key) throws Exception {
    XContentBuilder document = jsonBuilder().startObject();
    document.startObject(RuleNormalizer.RuleField.ACTIVE.key());
    document.startObject(key.toString());
    document.startObject(ActiveRuleField.PARAMS.key());
    document.startObject(param.getKey());
    indexField(ActiveRuleParamField.VALUE.key(), param.getValue(), document);
    document.endObject();
    document.endObject();
    document.endObject();
    document.endObject();
    document.endObject();

    /* Creating updateRequest */
    UpdateRequest request =  new UpdateRequest()
      .doc(document);
    request.docAsUpsert(true);
    return request;

  }

  @Override
  public UpdateRequest normalize(ActiveRuleDto rule) throws Exception {
    XContentBuilder document = jsonBuilder().startObject();

    document.startObject(RuleNormalizer.RuleField.ACTIVE.key());
    document.startObject(rule.getKey().toString());
    indexField(ActiveRuleField.OVERRIDE.key(), rule.doesOverride(), document);
    indexField(ActiveRuleField.INHERITANCE.key(), rule.getInheritance(), document);
    indexField(ActiveRuleField.PROFILE_ID.key(), rule.getProfileId(), document);
    indexField(ActiveRuleField.SEVERITY.key(), rule.getSeverityString(), document);
    indexField(ActiveRuleField.PARENT_ID.key(), rule.getParentId(), document);

    /* Done normalizing for Rule */
    document.endObject();
    document.endObject();

    /* Creating updateRequest */
    UpdateRequest request =  new UpdateRequest()
      .doc(document);
    request.docAsUpsert(true);
    return request;
  }
}
