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
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Cardinality;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.core.rule.RuleRuleTagDto;
import org.sonar.core.rule.RuleTagType;
import org.sonar.server.search.BaseNormalizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class RuleNormalizer extends BaseNormalizer<RuleDto, RuleKey> {

  private final RuleDao ruleDao;
  private final ActiveRuleDao activeRuleDao;

  public static enum RuleField {
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
    UPDATED_AT("updatedAt"),
    PARAMS("params"),
    ACTIVE("active");

    private final String key;

    private RuleField(final String key) {
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

  public static enum RuleParamField {
    NAME("name"),
    TYPE("type"),
    DESCRIPTION("description"),
    VALUE("value");

    private final String key;

    private RuleParamField(final String key) {
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

  public static enum ActiveRuleField {
    OVERRIDE("override"),
    INHERITANCE("inheritance"),
    NOTE_CREATED("noteCreatedAt"),
    NOTE_UPDATED("noteUpdatedAt"),
    NOTE_DATA("noteData"),
    NOTE_USER("noteUser"),
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

  public RuleNormalizer(RuleDao ruleDao, ActiveRuleDao activeRuleDao) {
    this.ruleDao = ruleDao;
    this.activeRuleDao = activeRuleDao;
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


    /* Normalize the tags */
    List<RuleRuleTagDto> tags = ruleDao.selectTagsByRuleId(rule.getId());
    if (tags != null && !tags.isEmpty()) {
      ArrayList<String> sys = new ArrayList<String>();
      ArrayList<String> admin = new ArrayList<String>();
      for (RuleRuleTagDto tag : tags) {
        if (tag.getType().equals(RuleTagType.SYSTEM)) {
          sys.add(tag.getTag());
        } else {
          admin.add(tag.getTag());
        }
      }
      if (!admin.isEmpty()) {
        document.array(RuleField.TAGS.key(), admin.toArray(new String[admin.size()]));
      }
      if (!sys.isEmpty()) {
        document.array(RuleField.SYSTEM_TAGS.key(), sys.toArray(new String[sys.size()]));
      }
    }

    /* Normalize the params */
    List<RuleParamDto> params = ruleDao.selectParametersByRuleId(rule.getId());
    Map<Integer, String> paramIdNameLookup = new HashMap<Integer, String>();
    if (!params.isEmpty()) {
      document.startArray(RuleField.PARAMS.key());
      for (RuleParamDto param : params) {
        paramIdNameLookup.put(param.getId(), param.getName());
        document.startObject();
        indexField(RuleParamField.NAME.key(), param.getName(), document);
        indexField(RuleParamField.TYPE.key(), param.getType(), document);
        indexField(RuleParamField.DESCRIPTION.key(), param.getDescription(), document);
        indexField(RuleParamField.VALUE.key(), param.getDefaultValue(), document);
        document.endObject();
      }
      document.endArray();
    }

    /* Normalize activeRules */
    List<ActiveRuleDto> activeRules = activeRuleDao.selectByRuleId(rule.getId());
    if (!activeRules.isEmpty()) {
      document.startArray(RuleField.ACTIVE.key());
      for (ActiveRuleDto activeRule : activeRules) {
        document.startObject();
        indexField(ActiveRuleField.OVERRIDE.key(), activeRule.doesOverride(), document);
        indexField(ActiveRuleField.INHERITANCE.key(), activeRule.getInheritance(), document);
        indexField(ActiveRuleField.NOTE_CREATED.key(), activeRule.getNoteCreatedAt(), document);
        indexField(ActiveRuleField.NOTE_UPDATED.key(), activeRule.getNoteUpdatedAt(), document);
        indexField(ActiveRuleField.NOTE_DATA.key(), activeRule.getNoteData(), document);
        indexField(ActiveRuleField.NOTE_USER.key(), activeRule.getNoteUserLogin(), document);
        indexField(ActiveRuleField.PROFILE_ID.key(), activeRule.getProfileId(), document);
        indexField(ActiveRuleField.SEVERITY.key(), activeRule.getSeverityString(), document);
        indexField(ActiveRuleField.PARENT_ID.key(), activeRule.getParentId(), document);

        /* Get all activeRuleParams */
        List<ActiveRuleParamDto> activeRuleParams = activeRuleDao.selectParamsByActiveRuleId(activeRule.getId());
        if(!activeRuleParams.isEmpty()) {
          document.startArray(ActiveRuleField.PARAMS.key());
          for (ActiveRuleParamDto param : activeRuleParams) {
            document.startObject();
            indexField(RuleParamField.NAME.key(), param.getKey(), document);
            indexField(RuleParamField.VALUE.key(), param.getValue(), document);
            document.endObject();
          }
          document.endArray();
        }
        document.endObject();
      }
      document.endArray();
    }

    /* Done normalizing for Rule */
    return document.endObject();
  }
}
