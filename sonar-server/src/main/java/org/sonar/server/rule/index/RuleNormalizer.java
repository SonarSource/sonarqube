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
package org.sonar.server.rule.index;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.elasticsearch.action.update.UpdateRequest;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Cardinality;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.search.BaseNormalizer;
import org.sonar.server.search.IndexDefinition;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RuleNormalizer extends BaseNormalizer<RuleDto, RuleKey> {

  public static enum RuleField {
    KEY("key"),
    REPOSITORY("repo"),
    NAME("name"),
    CREATED_AT("createdAt"),
    HTML_DESCRIPTION("htmlDesc"),
    SEVERITY("severity"),
    STATUS("status"),
    LANGUAGE("lang"),
    TAGS("tags"),
    SYSTEM_TAGS("sysTags"),
    INTERNAL_KEY("internalKey"),
    TEMPLATE("template"),
    UPDATED_AT("updatedAt"),
    PARAMS("params"),
    DEBT_FUNCTION_TYPE("debtRemFnType"),
    DEBT_FUNCTION_COEFFICIENT("debtRemFnCoefficient"),
    DEBT_FUNCTION_OFFSET("debtRemFnOffset"),
    SUB_CHARACTERISTIC("debtSubChar"),
    CHARACTERISTIC("debtChar"),
    NOTE("markdownNote"),
    NOTE_LOGIN("noteLogin"),
    NOTE_CREATED_AT("noteCreatedAt"),
    NOTE_UPDATED_AT("noteUpdatedAt"),
    _TAGS("_tags");

    private final String key;

    private RuleField(final String key) {
      this.key = key;
    }

    public String key() {
      return key;
    }

    public static RuleField fromKey(String key) {
      for (RuleField ruleField : RuleField.values()) {
        if (ruleField.key().equals(key)) {
          return ruleField;
        }
      }
      return null;
    }

    @Override
    public String toString() {
      return key;
    }

    public static final Set<String> ALL_KEYS = ImmutableSet.copyOf(Collections2.transform(Lists.newArrayList(RuleField.values()), new Function<RuleField, String>() {
      @Override
      public String apply(@Nullable RuleField input) {
        return input != null ? input.key : null;
      }
    }));
  }

  public static enum RuleParamField {
    NAME("name"),
    TYPE("type"),
    DESCRIPTION("description"),
    DEFAULT_VALUE("defaultValue");

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

  public RuleNormalizer(DbClient db) {
    super(IndexDefinition.RULE, db);
  }

  @Override
  public List<UpdateRequest> normalize(RuleKey key) {
    DbSession dbSession = db.openSession(false);
    List<UpdateRequest> requests = new ArrayList<UpdateRequest>();
    try {
      requests.addAll(normalize(db.ruleDao().getByKey(key, dbSession)));
      for (RuleParamDto param : db.ruleDao().findRuleParamsByRuleKey(key, dbSession)) {
        requests.addAll(normalize(param, key));
      }
    } finally {
      dbSession.close();
    }
    return requests;
  }

  @Override
  public List<UpdateRequest> normalize(RuleDto rule) {

    /** Update Fields */
    Map<String, Object> update = new HashMap<String, Object>();
    update.put(RuleField.KEY.key(), rule.getKey().toString());
    update.put(RuleField.REPOSITORY.key(), rule.getRepositoryKey());
    update.put(RuleField.NAME.key(), rule.getName());
    update.put(RuleField.CREATED_AT.key(), rule.getCreatedAt());
    update.put(RuleField.UPDATED_AT.key(), rule.getUpdatedAt());
    update.put(RuleField.HTML_DESCRIPTION.key(), rule.getDescription());
    update.put(RuleField.SEVERITY.key(), rule.getSeverityString());
    update.put(RuleField.STATUS.key(), rule.getStatus());
    update.put(RuleField.LANGUAGE.key(), rule.getLanguage());
    update.put(RuleField.INTERNAL_KEY.key(), rule.getConfigKey());
    update.put(RuleField.TEMPLATE.key(), rule.getCardinality() == Cardinality.MULTIPLE);

    update.put(RuleField.NOTE.key(), rule.getNoteData());
    update.put(RuleField.NOTE_LOGIN.key(), rule.getNoteUserLogin());
    update.put(RuleField.NOTE_CREATED_AT.key(), rule.getNoteCreatedAt());
    update.put(RuleField.NOTE_UPDATED_AT.key(), rule.getNoteUpdatedAt());

    //TODO Legacy ID in DTO should be Key
    CharacteristicDto characteristic = null;
    if (rule.getDefaultSubCharacteristicId() != null) {
      characteristic = db.debtCharacteristicDao().selectById(rule.getDefaultSubCharacteristicId());
    }
    if (rule.getSubCharacteristicId() != null) {
      characteristic = db.debtCharacteristicDao().selectById(rule.getSubCharacteristicId());
    }
    if (characteristic != null) {
      update.put(RuleField.SUB_CHARACTERISTIC.key(), characteristic.getKey());
      if (characteristic.getParentId() != null) {
        update.put(RuleField.CHARACTERISTIC.key(),
          db.debtCharacteristicDao().selectById(characteristic.getParentId()).getKey());
      }
    } else {
      update.put(RuleField.CHARACTERISTIC.key(), null);
      update.put(RuleField.SUB_CHARACTERISTIC.key(), null);
    }

    String dType = null, dCoefficient = null, dOffset = null;
    if (rule.getDefaultRemediationFunction() != null) {
      dType = rule.getDefaultRemediationFunction();
      dCoefficient = rule.getDefaultRemediationCoefficient();
      dOffset = rule.getDefaultRemediationOffset();
    }
    if (rule.getRemediationFunction() != null) {
      dType = rule.getRemediationFunction();
      dCoefficient = rule.getRemediationCoefficient();
      dOffset = rule.getRemediationOffset();
    }
    update.put(RuleField.DEBT_FUNCTION_TYPE.key(), dType);
    update.put(RuleField.DEBT_FUNCTION_COEFFICIENT.key(), dCoefficient);
    update.put(RuleField.DEBT_FUNCTION_OFFSET.key(), dOffset);
    update.put(RuleField.TAGS.key(), rule.getTags());
    update.put(RuleField.SYSTEM_TAGS.key(), rule.getSystemTags());
    update.put(RuleField._TAGS.key(), Sets.union(rule.getSystemTags(), rule.getTags()));


    /** Upsert elements */
    Map<String, Object> upsert = new HashMap<String, Object>(update);
    upsert.put(RuleField.KEY.key(), rule.getKey().toString());
    upsert.put(RuleField.PARAMS.key(), new ArrayList<String>());


    /** Creating updateRequest */
    return ImmutableList.of(new UpdateRequest()
      .doc(update)
      .upsert(upsert));
  }

  public List<UpdateRequest> normalize(RuleParamDto param, RuleKey key) {
    Map<String, Object> newParam = new HashMap<String, Object>();
    newParam.put("_id", param.getName());
    newParam.put(RuleParamField.NAME.key(), param.getName());
    newParam.put(RuleParamField.TYPE.key(), param.getType());
    newParam.put(RuleParamField.DESCRIPTION.key(), param.getDescription());
    newParam.put(RuleParamField.DEFAULT_VALUE.key(), param.getDefaultValue());

    return ImmutableList.of(this.nestedUpsert(RuleField.PARAMS.key(),
      param.getName(), newParam).id(key.toString()));
  }
}
