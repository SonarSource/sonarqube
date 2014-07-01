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
package org.sonar.server.qualityprofile.index;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.elasticsearch.action.support.replication.ReplicationType;
import org.elasticsearch.action.update.UpdateRequest;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.search.BaseNormalizer;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.IndexField;
import org.sonar.server.search.Indexable;
import org.sonar.server.search.es.ListUpdate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ActiveRuleNormalizer extends BaseNormalizer<ActiveRuleDto, ActiveRuleKey> {

  public static class ActiveRuleField extends Indexable {

    public static final IndexField KEY = addSortableAndSearchable(IndexField.Type.STRING, "key");
    public static final IndexField INHERITANCE = add(IndexField.Type.STRING, "inheritance");
    public static final IndexField PROFILE_KEY = add(IndexField.Type.STRING, "profile");
    public static final IndexField SEVERITY = add(IndexField.Type.STRING, "severity");
    public static final IndexField PARENT_KEY = add(IndexField.Type.STRING, "parentKey");
    public static final IndexField RULE_KEY = add(IndexField.Type.STRING, "ruleKey");
    public static final IndexField PARAMS = addEmbedded("params", ActiveRuleParamField.ALL_FIELDS);

    public static Set<IndexField> ALL_FIELDS = getAllFields();

    private static Set<IndexField> getAllFields() {
      Set<IndexField> fields = new HashSet<IndexField>();
      for (Field classField : ActiveRuleField.class.getDeclaredFields()) {
        if (classField.getType().isAssignableFrom(IndexField.class)) {
          try {
            fields.add(IndexField.class.cast(classField.get(null)));
          } catch (IllegalAccessException e) {
            throw new IllegalStateException("Can not introspect active rule fields", e);
          }
        }
      }
      return fields;
    }

  }

  public static class ActiveRuleParamField extends Indexable {
    public static final IndexField NAME = add(IndexField.Type.STRING, "name");
    public static final IndexField VALUE = add(IndexField.Type.STRING, "value");
    public static final Set<IndexField> ALL_FIELDS = getAllFields();

    private static Set<IndexField> getAllFields() {
      Set<IndexField> fields = new HashSet<IndexField>();
      for (Field classField : ActiveRuleParamField.class.getDeclaredFields()) {
        if (classField.getType().isAssignableFrom(IndexField.class)) {
          try {
            fields.add(IndexField.class.cast(classField.get(null)));
          } catch (IllegalAccessException e) {
            throw new IllegalStateException("Can not introspect active rule param fields", e);
          }
        }
      }
      return fields;
    }
  }

  public ActiveRuleNormalizer(DbClient db) {
    super(IndexDefinition.ACTIVE_RULE, db);
  }

  @Override
  public List<UpdateRequest> normalize(ActiveRuleKey key) {
    DbSession dbSession = db.openSession(false);
    List<UpdateRequest> requests = new ArrayList<UpdateRequest>();
    try {
      ActiveRuleDto activeRule = db.activeRuleDao().getByKey(dbSession, key);
      requests.addAll(normalize(activeRule));
      for (ActiveRuleParamDto param : db.activeRuleDao().findParamsByActiveRuleKey(dbSession, key)) {
        requests.addAll(normalizeNested(param, key));
      }
    } finally {
      dbSession.close();
    }
    return requests;
  }

  @Override
  public List<UpdateRequest> normalize(ActiveRuleDto activeRuleDto) {
    ActiveRuleKey key = activeRuleDto.getKey();
    Preconditions.checkArgument(key != null, "Cannot normalize ActiveRuleDto with null key");

    Map<String, Object> newRule = new HashMap<String, Object>();
    newRule.put("_parent", key.ruleKey().toString());
    newRule.put(ActiveRuleField.RULE_KEY.field(), key.ruleKey().toString());
    newRule.put(ActiveRuleField.KEY.field(), key.toString());
    newRule.put(ActiveRuleField.INHERITANCE.field(),
      (activeRuleDto.getInheritance() != null) ?
        activeRuleDto.getInheritance() :
        ActiveRule.Inheritance.NONE.name());
    newRule.put(ActiveRuleField.SEVERITY.field(), activeRuleDto.getSeverityString());

    DbSession session = db.openSession(false);
    try {
      // TODO because DTO uses legacy ID pattern
      QualityProfileDto profile = db.qualityProfileDao().getById(activeRuleDto.getProfileId(), session);
      newRule.put(ActiveRuleField.PROFILE_KEY.field(), profile.getKey());

      // TODO this should be generated by RegisterRule and modified in DTO.
      String parentKey = null;
      Integer parentId = activeRuleDto.getParentId();
      if (parentId != null) {
        ActiveRuleDto parentDto = db.activeRuleDao().getById(session, parentId);
        parentKey = parentDto.getKey().toString();
      }
      newRule.put(ActiveRuleField.PARENT_KEY.field(), parentKey);
    } finally {
      session.close();
    }

    Map<String, Object> upsert = new HashMap<String, Object>(newRule);
    upsert.put(ActiveRuleField.PARAMS.field(), new ArrayList());

    /* Creating updateRequest */
    return ImmutableList.of(new UpdateRequest()
      .replicationType(ReplicationType.ASYNC)
      .routing(key.ruleKey().toString())
      .id(activeRuleDto.getKey().toString())
      .parent(activeRuleDto.getKey().ruleKey().toString())
      .doc(newRule)
      .upsert(upsert));
  }

  @Override
  public List<UpdateRequest> normalizeNested(Object object, ActiveRuleKey key) {
    Preconditions.checkArgument(key != null, "key cannot be null");
    if (object.getClass().isAssignableFrom(ActiveRuleParamDto.class)) {
      return nestedUpdate((ActiveRuleParamDto) object, key);
    } else {
      throw new IllegalStateException("Cannot normalize object of type '" + object.getClass() + "' in current context");
    }
  }

  @Override
  public List<UpdateRequest> deleteNested(Object object, ActiveRuleKey key) {
    Preconditions.checkArgument(key != null, "key of Rule must be set");
    if (object.getClass().isAssignableFrom(ActiveRuleParamDto.class)) {
      return nestedDelete((ActiveRuleParamDto) object, key);
    } else {
      throw new IllegalStateException("Cannot normalize object of type '" + object.getClass() + "' in current context");
    }
  }

  private List<UpdateRequest> nestedUpdate(ActiveRuleParamDto param, ActiveRuleKey key) {
    Preconditions.checkArgument(key != null, "Cannot normalize ActiveRuleParamDto for null key of ActiveRule");

    Map<String, Object> newParam = new HashMap<String, Object>();
    newParam.put(ActiveRuleParamField.NAME.field(), param.getKey());
    newParam.put(ActiveRuleParamField.VALUE.field(), param.getValue());

    return ImmutableList.of(new UpdateRequest()
      .replicationType(ReplicationType.ASYNC)
      .routing(key.ruleKey().toString())
      .id(key.toString())
      .script(ListUpdate.NAME)
      .addScriptParam(ListUpdate.FIELD, ActiveRuleField.PARAMS.field())
      .addScriptParam(ListUpdate.VALUE, newParam)
      .addScriptParam(ListUpdate.ID_FIELD, ActiveRuleParamField.NAME.field())
      .addScriptParam(ListUpdate.ID_VALUE, param.getKey())
      );
  }

  private List<UpdateRequest> nestedDelete(ActiveRuleParamDto param, ActiveRuleKey key) {
    return ImmutableList.of(new UpdateRequest()
      .replicationType(ReplicationType.ASYNC)
      .routing(key.ruleKey().toString())
      .id(key.toString())
      .script(ListUpdate.NAME)
      .addScriptParam(ListUpdate.FIELD, ActiveRuleField.PARAMS.field())
      .addScriptParam(ListUpdate.VALUE, null)
      .addScriptParam(ListUpdate.ID_FIELD, ActiveRuleParamField.NAME.field())
      .addScriptParam(ListUpdate.ID_VALUE, param.getKey())
      );
  }
}
