/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.qualityprofile;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import org.apache.ibatis.session.SqlSession;
import org.elasticsearch.common.io.BytesStream;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.profiling.Profiling.Level;
import org.sonar.core.profiling.StopWatch;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.server.es.ESIndex;
import org.sonar.server.es.SearchQuery;
import org.sonar.server.rule.RuleDocument;
import org.sonar.server.rule.RuleRegistry;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class ESActiveRule {

  public static final String TYPE_ACTIVE_RULE = "active_rule";
  private final ESIndex esIndex;
  private final ActiveRuleDao activeRuleDao;
  private final MyBatis myBatis;
  private final Profiling profiling;

  public ESActiveRule(ESIndex esIndex, ActiveRuleDao activeRuleDao, MyBatis myBatis, Profiling profiling) {
    this.esIndex = esIndex;
    this.activeRuleDao = activeRuleDao;
    this.myBatis = myBatis;
    this.profiling = profiling;
  }

  public void start() {
    esIndex.addMappingFromClasspath(RuleRegistry.INDEX_RULES, TYPE_ACTIVE_RULE, "/org/sonar/server/es/config/mappings/active_rule_mapping.json");
  }

  public void bulkRegisterActiveRules() {
    SqlSession session = myBatis.openSession();
    try {

      StopWatch bulkWatch = startWatch();
      List<ActiveRuleDto> activeRules = activeRuleDao.selectAll(session);
      List<ActiveRuleParamDto> activeRuleParams = activeRuleDao.selectAllParams(session);
      bulkWatch.stop(String.format("Loaded %d active rules from DB", activeRules.size()));

      Multimap<Integer, ActiveRuleParamDto> paramsByActiveRule = ArrayListMultimap.create();
      for (ActiveRuleParamDto param : activeRuleParams) {
        paramsByActiveRule.put(param.getActiveRuleId(), param);
      }

      String[] ids = bulkIndexActiveRules(activeRules, paramsByActiveRule);
      removeDeletedActiveRules(ids);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void bulkIndexProfile(int profileId, SqlSession session) {
    bulkIndexActiveRules(activeRuleDao.selectByProfileId(profileId, session), session);
  }

  public void bulkIndexActiveRuleIds(List<Integer> activeRulesIds, SqlSession session) {
    bulkIndexActiveRules(activeRuleDao.selectByIds(activeRulesIds, session), session);
  }

  public void bulkIndexActiveRules(List<Integer> ids) {
    SqlSession session = myBatis.openSession();
    try {
      bulkIndexActiveRuleIds(ids, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteActiveRulesFromProfile(int profileId) {
    esIndex.client().prepareDeleteByQuery(RuleRegistry.INDEX_RULES).setTypes(ESActiveRule.TYPE_ACTIVE_RULE)
      .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
        FilterBuilders.termFilter(ActiveRuleDocument.FIELD_PROFILE_ID, profileId)))
      .execute().actionGet();
  }

  public String[] bulkIndexActiveRules(List<ActiveRuleDto> activeRules, Multimap<Integer, ActiveRuleParamDto> paramsByActiveRule) {
    StopWatch bulkWatch = startWatch();
    try {
      int size = activeRules.size();
      String[] ids = new String[size];
      BytesStream[] docs = new BytesStream[size];
      String[] parentIds = new String[size];
      int index = 0;

      for (ActiveRuleDto activeRule : activeRules) {
        ids[index] = activeRule.getId().toString();
        docs[index] = activeRuleDocument(activeRule, paramsByActiveRule.get(activeRule.getId()));
        parentIds[index] = activeRule.getRulId().toString();
        index++;
      }

      if (!activeRules.isEmpty()) {
        esIndex.bulkIndex(RuleRegistry.INDEX_RULES, ESActiveRule.TYPE_ACTIVE_RULE, ids, docs, parentIds);
      }
      bulkWatch.stop(String.format("Indexed %d active rules", size));
      return ids;
    } catch (IOException e) {
      bulkWatch.stop("Failed to indes active rules");
      throw new IllegalStateException("Unable to index active rules", e);
    } finally {
    }
  }

  public void save(ActiveRuleDto activeRule, Collection<ActiveRuleParamDto> params) {
    try {
      esIndex.putSynchronous(RuleRegistry.INDEX_RULES, ESActiveRule.TYPE_ACTIVE_RULE, Long.toString(activeRule.getId()), activeRuleDocument(activeRule, params), Long.toString(activeRule.getRulId()));
    } catch (IOException ioexception) {
      throw new IllegalStateException("Unable to index active rule with id=" + activeRule.getId(), ioexception);
    }
  }

  public void deleteActiveRules(List<Integer> activeRuleIds) {
    List<String> indexIds = newArrayList();
    for (Integer ruleId : activeRuleIds) {
      indexIds.add(ruleId.toString());
    }
    bulkDeleteActiveRules(indexIds);
  }

  private void bulkDeleteActiveRules(List<String> indexIds) {
    if (!indexIds.isEmpty()) {
      esIndex.bulkDelete(RuleRegistry.INDEX_RULES, ESActiveRule.TYPE_ACTIVE_RULE, indexIds.toArray(new String[0]));
    }
  }

  private void bulkIndexActiveRules(List<ActiveRuleDto> activeRules, SqlSession session) {
    Multimap<Integer, ActiveRuleParamDto> paramsByActiveRule = ArrayListMultimap.create();
    List<Integer> activeRulesIdList = newArrayList(Iterables.transform(activeRules, new Function<ActiveRuleDto, Integer>() {
      @Override
      public Integer apply(ActiveRuleDto input) {
        return input.getId();
      }
    }));
    for (ActiveRuleParamDto param : activeRuleDao.selectParamsByActiveRuleIds(activeRulesIdList, session)) {
      paramsByActiveRule.put(param.getActiveRuleId(), param);
    }
    bulkIndexActiveRules(activeRules, paramsByActiveRule);
  }

  private void removeDeletedActiveRules(String[] ids) {
    TimeProfiler profiler = new TimeProfiler();
    List<String> indexIds = esIndex.findDocumentIds(SearchQuery.create().index(RuleRegistry.INDEX_RULES).type(ESActiveRule.TYPE_ACTIVE_RULE));
    indexIds.removeAll(newArrayList(ids));
    profiler.start("Remove deleted active rule documents");
    bulkDeleteActiveRules(indexIds);
    profiler.stop();
  }

  private XContentBuilder activeRuleDocument(ActiveRuleDto activeRule, Collection<ActiveRuleParamDto> params) throws IOException {
    XContentBuilder document = XContentFactory.jsonBuilder()
      .startObject()
      .field(ActiveRuleDocument.FIELD_ID, activeRule.getId())
      .field(ActiveRuleDocument.FIELD_ACTIVE_RULE_PARENT_ID, activeRule.getParentId())
      .field(ActiveRuleDocument.FIELD_SEVERITY, activeRule.getSeverityString())
      .field(ActiveRuleDocument.FIELD_PROFILE_ID, activeRule.getProfileId())
      .field(ActiveRuleDocument.FIELD_INHERITANCE, activeRule.getInheritance());
    if (activeRule.getNoteData() != null || activeRule.getNoteUserLogin() != null) {
      document.startObject(RuleDocument.FIELD_NOTE)
        .field(ActiveRuleDocument.FIELD_NOTE_DATA, activeRule.getNoteData())
        .field(ActiveRuleDocument.FIELD_NOTE_USER_LOGIN, activeRule.getNoteUserLogin())
        .field(ActiveRuleDocument.FIELD_NOTE_CREATED_AT, activeRule.getNoteCreatedAt())
        .field(ActiveRuleDocument.FIELD_NOTE_UPDATED_AT, activeRule.getNoteUpdatedAt())
        .endObject();
    }
    if (!params.isEmpty()) {
      document.startArray(ActiveRuleDocument.FIELD_PARAMS);
      for (ActiveRuleParamDto param : params) {
        document.startObject()
          .field(ActiveRuleDocument.FIELD_PARAM_KEY, param.getKey())
          .field(ActiveRuleDocument.FIELD_PARAM_VALUE, param.getValue())
          .endObject();
      }
      document.endArray();
    }
    document.endObject();
    return document;
  }

  private StopWatch startWatch() {
    return profiling.start("qprofile", Level.BASIC);
  }
}
