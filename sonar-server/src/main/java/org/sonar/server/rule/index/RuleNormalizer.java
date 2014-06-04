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

import com.google.common.collect.ImmutableList;
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
import org.sonar.server.search.IndexField;
import org.sonar.server.search.Indexable;
import org.sonar.server.search.es.ListUpdate;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RuleNormalizer extends BaseNormalizer<RuleDto, RuleKey> {

  public static final class RuleParamField extends Indexable {
    public static IndexField NAME = add(IndexField.Type.STRING, "name");

    public static IndexField TYPE = add(IndexField.Type.STRING, "type");
    public static IndexField DESCRIPTION = addSearchable(IndexField.Type.TEXT, "description");
    public static IndexField DEFAULT_VALUE = add(IndexField.Type.STRING, "defaultValue");

    public static Set<IndexField> ALL_FIELDS = getAllFields();

    private static Set<IndexField> getAllFields() {
      Set<IndexField> fields = new HashSet<IndexField>();
      for (Field classField : RuleParamField.class.getDeclaredFields()) {
        if (Modifier.isStatic(classField.getModifiers())) {
          try {
            fields.add(IndexField.class.cast(classField.get(null)));
          } catch (IllegalAccessException e) {
            e.printStackTrace();
          }
        }
      }
      return fields;
    }
  }

  public static final class RuleField extends Indexable {

    public static IndexField KEY = addSortableAndSearchable(IndexField.Type.STRING, "key");
    public static IndexField REPOSITORY = add(IndexField.Type.STRING, "repo");
    public static IndexField NAME = addSortableAndSearchable(IndexField.Type.STRING, "name");

    public static IndexField CREATED_AT = addSortable(IndexField.Type.DATE, "createdAt");
    public static IndexField UPDATED_AT = addSortable(IndexField.Type.DATE, "updatedAt");
    public static IndexField HTML_DESCRIPTION = addSearchable(IndexField.Type.TEXT, "htmlDesc");
    public static IndexField SEVERITY = add(IndexField.Type.STRING, "severity");
    public static IndexField STATUS = add(IndexField.Type.STRING, "status");
    public static IndexField LANGUAGE = add(IndexField.Type.STRING, "lang");
    public static IndexField TAGS = add(IndexField.Type.STRING, "tags");
    public static IndexField SYSTEM_TAGS = add(IndexField.Type.STRING, "sysTags");
    public static IndexField INTERNAL_KEY = add(IndexField.Type.STRING, "internalKey");
    public static IndexField IS_TEMPLATE = add(IndexField.Type.BOOLEAN, "isTemplate");
    public static IndexField TEMPLATE_KEY = add(IndexField.Type.STRING, "templateKey");
    public static IndexField DEBT_FUNCTION_TYPE = add(IndexField.Type.STRING, "debtRemFnType");
    public static IndexField DEBT_FUNCTION_COEFFICIENT = add(IndexField.Type.STRING, "debtRemFnCoefficient");
    public static IndexField DEBT_FUNCTION_OFFSET = add(IndexField.Type.STRING, "debtRemFnOffset");
    public static IndexField SUB_CHARACTERISTIC = add(IndexField.Type.STRING, "debtSubChar");
    public static IndexField CHARACTERISTIC = add(IndexField.Type.STRING, "debtChar");
    public static IndexField NOTE = add(IndexField.Type.TEXT, "markdownNote");
    public static IndexField NOTE_LOGIN = add(IndexField.Type.STRING, "noteLogin");
    public static IndexField NOTE_CREATED_AT = add(IndexField.Type.DATE, "noteCreatedAt");
    public static IndexField NOTE_UPDATED_AT = add(IndexField.Type.DATE, "noteUpdatedAt");
    public static IndexField _TAGS = addSearchable(IndexField.Type.STRING, "_tags");
    public static IndexField PARAMS = addEmbedded("params", RuleParamField.ALL_FIELDS);


    public static Set<IndexField> ALL_FIELDS = getAllFields();

    private static Set<IndexField> getAllFields() {
      Set<IndexField> fields = new HashSet<IndexField>();
      for (Field classField : RuleField.class.getDeclaredFields()) {
        if (classField.getType().isAssignableFrom(IndexField.class)) {
          //Modifier.isStatic(classField.getModifiers())
          try {
            fields.add(IndexField.class.cast(classField.get(null)));
          } catch (IllegalAccessException e) {
            e.printStackTrace();
          }
        }
      }
      return fields;
    }

    public static IndexField of(String fieldName) {
      for (IndexField field : ALL_FIELDS) {
        if (field.field().equals(fieldName)) {
          return field;
        }
      }
      return null;
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
      requests.addAll(normalize(db.ruleDao().getNullableByKey(dbSession, key)));
      for (RuleParamDto param : db.ruleDao().findRuleParamsByRuleKey(dbSession, key)) {
        requests.addAll(normalize(param, key));
      }
    } finally {
      dbSession.close();
    }
    return requests;
  }

  @Override
  public List<UpdateRequest> normalize(RuleDto rule) {

    DbSession session = db.openSession(false);
    try {

      /** Update Fields */
      Map<String, Object> update = new HashMap<String, Object>();
      update.put(RuleField.KEY.field(), rule.getKey().toString());
      update.put(RuleField.REPOSITORY.field(), rule.getRepositoryKey());
      update.put(RuleField.NAME.field(), rule.getName());
      update.put(RuleField.CREATED_AT.field(), rule.getCreatedAt());
      update.put(RuleField.UPDATED_AT.field(), rule.getUpdatedAt());
      update.put(RuleField.HTML_DESCRIPTION.field(), rule.getDescription());
      update.put(RuleField.SEVERITY.field(), rule.getSeverityString());
      update.put(RuleField.STATUS.field(), rule.getStatus().name());
      update.put(RuleField.LANGUAGE.field(), rule.getLanguage());
      update.put(RuleField.INTERNAL_KEY.field(), rule.getConfigKey());
      update.put(RuleField.IS_TEMPLATE.field(), rule.getCardinality() == Cardinality.MULTIPLE);

      update.put(RuleField.NOTE.field(), rule.getNoteData());
      update.put(RuleField.NOTE_LOGIN.field(), rule.getNoteUserLogin());
      update.put(RuleField.NOTE_CREATED_AT.field(), rule.getNoteCreatedAt());
      update.put(RuleField.NOTE_UPDATED_AT.field(), rule.getNoteUpdatedAt());

      //TODO Legacy PARENT_ID in DTO should be parent_key
      Integer parentId = rule.getParentId();
      if (parentId != null) {
        RuleDto templateRule = db.ruleDao().getById(session, parentId);
        update.put(RuleField.TEMPLATE_KEY.field(), templateRule.getKey().toString());
      } else {
        update.put(RuleField.TEMPLATE_KEY.field(), null);
      }

      //TODO Legacy ID in DTO should be Key
      CharacteristicDto characteristic = null;
      if (rule.getDefaultSubCharacteristicId() != null) {
        characteristic = db.debtCharacteristicDao().selectById(rule.getDefaultSubCharacteristicId(), session);
      }
      if (rule.getSubCharacteristicId() != null) {
        characteristic = db.debtCharacteristicDao().selectById(rule.getSubCharacteristicId(), session);
      }
      if (characteristic != null && characteristic.getId() != -1) {
        update.put(RuleField.SUB_CHARACTERISTIC.field(), characteristic.getKey());
        if (characteristic.getParentId() != null) {
          update.put(RuleField.CHARACTERISTIC.field(),
            db.debtCharacteristicDao().selectById(characteristic.getParentId(), session).getKey());
        }
      } else {
        update.put(RuleField.CHARACTERISTIC.field(), null);
        update.put(RuleField.SUB_CHARACTERISTIC.field(), null);
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
      update.put(RuleField.DEBT_FUNCTION_TYPE.field(), dType);
      update.put(RuleField.DEBT_FUNCTION_COEFFICIENT.field(), dCoefficient);
      update.put(RuleField.DEBT_FUNCTION_OFFSET.field(), dOffset);
      update.put(RuleField.TAGS.field(), rule.getTags());
      update.put(RuleField.SYSTEM_TAGS.field(), rule.getSystemTags());
      update.put(RuleField._TAGS.field(), Sets.union(rule.getSystemTags(), rule.getTags()));


      /** Upsert elements */
      Map<String, Object> upsert = new HashMap<String, Object>(update);
      upsert.put(RuleField.KEY.field(), rule.getKey().toString());
      upsert.put(RuleField.PARAMS.field(), new ArrayList<String>());


      /** Creating updateRequest */
      return ImmutableList.of(new UpdateRequest()
        .doc(update)
        .upsert(upsert));

    } finally {
      session.close();
    }
  }

  public List<UpdateRequest> normalize(RuleParamDto param, RuleKey key) {

    Map<String, Object> newParam = new HashMap<String, Object>();
    newParam.put("_id", param.getName());
    newParam.put(RuleParamField.NAME.field(), param.getName());
    newParam.put(RuleParamField.TYPE.field(), param.getType());
    newParam.put(RuleParamField.DESCRIPTION.field(), param.getDescription());
    newParam.put(RuleParamField.DEFAULT_VALUE.field(), param.getDefaultValue());

    return ImmutableList.of(new UpdateRequest()
        .id(key.toString())
        .script(ListUpdate.NAME)
        .addScriptParam(ListUpdate.FIELD, RuleField.PARAMS.field())
        .addScriptParam(ListUpdate.VALUE, newParam)
        .addScriptParam(ListUpdate.ID, param.getName())
    );
  }
}
