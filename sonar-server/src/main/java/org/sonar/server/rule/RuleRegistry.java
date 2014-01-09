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

package org.sonar.server.rule;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.ibatis.session.SqlSession;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.io.BytesStream;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Rule;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.search.SearchIndex;
import org.sonar.server.search.SearchQuery;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Fill search index with rules
 *
 * @since 4.1
 */
public class RuleRegistry {

  public static final String INDEX_RULES = "rules";
  public static final String TYPE_RULE = "rule";
  public static final String TYPE_ACTIVE_RULE = "active_rule";

  private static final String PARAM_NAMEORKEY = "nameOrKey";
  private static final String PARAM_STATUS = "status";

  private final SearchIndex searchIndex;
  private final RuleDao ruleDao;
  private final ActiveRuleDao activeRuleDao;
  private final MyBatis myBatis;

  public RuleRegistry(SearchIndex searchIndex, RuleDao ruleDao, ActiveRuleDao activeRuleDao, MyBatis myBatis) {
    this.searchIndex = searchIndex;
    this.ruleDao = ruleDao;
    this.activeRuleDao = activeRuleDao;
    this.myBatis = myBatis;
  }

  public void start() {
    searchIndex.addMappingFromClasspath(INDEX_RULES, TYPE_RULE, "/com/sonar/search/rule_mapping.json");
    searchIndex.addMappingFromClasspath(INDEX_RULES, TYPE_ACTIVE_RULE, "/com/sonar/search/active_rule_mapping.json");
  }

  public void bulkRegisterRules() {
    TimeProfiler profiler = new TimeProfiler();

    profiler.start("Rebuilding rules index - query");
    List<RuleDto> rules = ruleDao.selectNonManual();
    List<RuleParamDto> flatParams = ruleDao.selectParameters();
    profiler.stop();

    Multimap<Integer, RuleParamDto> paramsByRule = ArrayListMultimap.create();
    for (RuleParamDto param : flatParams) {
      paramsByRule.put(param.getRuleId(), param);
    }

    String[] ids = bulkIndexRules(rules, paramsByRule);
    removeDeletedRules(ids);
  }

  public void bulkRegisterActiveRules() {
    SqlSession session = myBatis.openSession();
    try {
      TimeProfiler profiler = new TimeProfiler();
      profiler.start("Rebuilding active rules index - query");

      List<ActiveRuleDto> activeRules = activeRuleDao.selectAll(session);
      List<ActiveRuleParamDto> activeRuleParams = activeRuleDao.selectAllParams(session);
      profiler.stop();

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

  /**
   * <p>Find rule IDs matching the given criteria.</p>
   *
   * @param query <p>A collection of (optional) criteria with the following meaning:
   *              <ul>
   *              <li><em>nameOrKey</em>: will be used as a query string over the "name" field</li>
   *              <li><em>&lt;anyField&gt;</em>: will be used to match the given field against the passed value(s);
   *              mutiple values must be separated by the '<code>|</code>' (vertical bar) character</li>
   *              </ul>
   *              </p>
   * @return
   */
  public List<Integer> findIds(Map<String, String> query) {
    Map<String, String> params = Maps.newHashMap(query);

    SearchQuery searchQuery = SearchQuery.create();
    searchQuery.index(INDEX_RULES).type(TYPE_RULE).scrollSize(500);

    if (params.containsKey(PARAM_NAMEORKEY)) {
      searchQuery.searchString(params.remove(PARAM_NAMEORKEY));
    }
    if (!params.containsKey(PARAM_STATUS)) {
      searchQuery.notField(PARAM_STATUS, Rule.STATUS_REMOVED);
    }

    for (Map.Entry<String, String> param : params.entrySet()) {
      searchQuery.field(param.getKey(), param.getValue().split("\\|"));
    }

    try {
      List<Integer> result = newArrayList();
      for (String docId : searchIndex.findDocumentIds(searchQuery)) {
        result.add(Integer.parseInt(docId));
      }
      return result;
    } catch (ElasticSearchException searchException) {
      throw new IllegalArgumentException("Unable to perform search, please check query", searchException);
    }
  }

  /**
   * Create or update definition of rule identified by <code>ruleId</code>
   *
   * @param ruleId
   */
  public void saveOrUpdate(int ruleId) {
    RuleDto rule = ruleDao.selectById(ruleId);
    Collection<RuleParamDto> params = ruleDao.selectParameters(rule.getId());
    save(rule, params);
  }

  public void save(RuleDto rule, Collection<RuleParamDto> params) {
    try {
      searchIndex.putSynchronous(INDEX_RULES, TYPE_RULE, Long.toString(rule.getId()), ruleDocument(rule, params));
    } catch (IOException ioexception) {
      throw new IllegalStateException("Unable to index rule with id=" + rule.getId(), ioexception);
    }
  }

  public void save(ActiveRuleDto activeRule, Collection<ActiveRuleParamDto> params) {
    try {
      searchIndex.putSynchronous(INDEX_RULES, TYPE_ACTIVE_RULE, Long.toString(activeRule.getId()), activeRuleDocument(activeRule, params), Long.toString(activeRule.getRulId()));
    } catch (IOException ioexception) {
      throw new IllegalStateException("Unable to index active rule with id=" + activeRule.getId(), ioexception);
    }
  }

  private String[] bulkIndexRules(List<RuleDto> rules, Multimap<Integer, RuleParamDto> paramsByRule) {
    try {
      String[] ids = new String[rules.size()];
      BytesStream[] docs = new BytesStream[rules.size()];
      int index = 0;
      TimeProfiler profiler = new TimeProfiler();
      profiler.start("Build rules documents");
      for (RuleDto rule : rules) {
        ids[index] = rule.getId().toString();
        docs[index] = ruleDocument(rule, paramsByRule.get(rule.getId()));
        index++;
      }
      profiler.stop();

      if (!rules.isEmpty()) {
        profiler.start("Index rules");
        searchIndex.bulkIndex(INDEX_RULES, TYPE_RULE, ids, docs);
        profiler.stop();
      }
      return ids;
    } catch (IOException ioe) {
      throw new IllegalStateException("Unable to index rules", ioe);
    }
  }

  private void removeDeletedRules(String[] ids) {
    List<String> indexIds = searchIndex.findDocumentIds(SearchQuery.create().index(INDEX_RULES).type(TYPE_RULE));
    indexIds.removeAll(Arrays.asList(ids));
    TimeProfiler profiler = new TimeProfiler();
    if (!indexIds.isEmpty()) {
      profiler.start("Remove deleted rule documents");
      searchIndex.bulkDelete(INDEX_RULES, TYPE_RULE, indexIds.toArray(new String[0]));
      profiler.stop();
    }
  }

  public void deleteActiveRules(List<Integer> activeRuleIds) {
    List<String> indexIds = newArrayList();
    for (Integer ruleId : activeRuleIds) {
      indexIds.add(ruleId.toString());
    }
    bulkDeleteActiveRules(indexIds);
  }

  protected void bulkDeleteActiveRules(List<String> indexIds) {
    if (!indexIds.isEmpty()) {
      searchIndex.bulkDelete(INDEX_RULES, TYPE_ACTIVE_RULE, indexIds.toArray(new String[0]));
    }
  }

  public String[] bulkIndexActiveRules(List<ActiveRuleDto> activeRules, Multimap<Integer, ActiveRuleParamDto> paramsByActiveRule) {
    try {
      int size = activeRules.size();
      String[] ids = new String[size];
      BytesStream[] docs = new BytesStream[size];
      String[] parentIds = new String[size];
      int index = 0;

      TimeProfiler profiler = new TimeProfiler();
      profiler.start("Build active rules documents");
      for (ActiveRuleDto activeRule : activeRules) {
        ids[index] = activeRule.getId().toString();
        docs[index] = activeRuleDocument(activeRule, paramsByActiveRule.get(activeRule.getId()));
        parentIds[index] = activeRule.getRulId().toString();
        index++;
      }
      profiler.stop();

      if (!activeRules.isEmpty()) {
        profiler.start("Index active rules");
        searchIndex.bulkIndex(INDEX_RULES, TYPE_ACTIVE_RULE, ids, docs, parentIds);
        profiler.stop();
      }
      return ids;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to index active rules", e);
    }
  }

  public void bulkIndexActiveRules(List<Integer> ids, SqlSession session) {
    List<ActiveRuleDto> activeRules = activeRuleDao.selectByIds(ids, session);
    Multimap<Integer, ActiveRuleParamDto> paramsByActiveRule = ArrayListMultimap.create();
    for (ActiveRuleParamDto param : activeRuleDao.selectParamsByActiveRuleIds(ids, session)) {
      paramsByActiveRule.put(param.getActiveRuleId(), param);
    }
    bulkIndexActiveRules(activeRules, paramsByActiveRule);
  }

  public void bulkIndexActiveRules(List<Integer> ids) {
    SqlSession session = myBatis.openSession();
    try {
      bulkIndexActiveRules(ids, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void removeDeletedActiveRules(String[] ids) {
    TimeProfiler profiler = new TimeProfiler();
    List<String> indexIds = searchIndex.findDocumentIds(SearchQuery.create().index(INDEX_RULES).type(TYPE_ACTIVE_RULE));
    indexIds.removeAll(newArrayList(ids));
    profiler.start("Remove deleted active rule documents");
    bulkDeleteActiveRules(indexIds);
    profiler.stop();
  }

  private XContentBuilder ruleDocument(RuleDto rule, Collection<RuleParamDto> params) throws IOException {
    XContentBuilder document = XContentFactory.jsonBuilder()
      .startObject()
      .field(RuleDocument.FIELD_ID, rule.getId())
      .field(RuleDocument.FIELD_KEY, rule.getRuleKey())
      .field(RuleDocument.FIELD_LANGUAGE, rule.getLanguage())
      .field(RuleDocument.FIELD_NAME, rule.getName())
      .field(RuleDocument.FIELD_DESCRIPTION, rule.getDescription())
      .field(RuleDocument.FIELD_PARENT_KEY, rule.getParentId() == null ? null : rule.getParentId())
      .field(RuleDocument.FIELD_REPOSITORY_KEY, rule.getRepositoryKey())
      .field(RuleDocument.FIELD_SEVERITY, Severity.get(rule.getSeverity()))
      .field(RuleDocument.FIELD_STATUS, rule.getStatus())
      .field(RuleDocument.FIELD_CARDINALITY, rule.getCardinality())
      .field(RuleDocument.FIELD_CREATED_AT, rule.getCreatedAt())
      .field(RuleDocument.FIELD_UPDATED_AT, rule.getUpdatedAt());
    if (rule.getNoteData() != null || rule.getNoteUserLogin() != null) {
      document.startObject(RuleDocument.FIELD_NOTE)
        .field(RuleDocument.FIELD_NOTE_DATA, rule.getNoteData())
        .field(RuleDocument.FIELD_NOTE_USER_LOGIN, rule.getNoteUserLogin())
        .field(RuleDocument.FIELD_NOTE_CREATED_AT, rule.getNoteCreatedAt())
        .field(RuleDocument.FIELD_NOTE_UPDATED_AT, rule.getNoteUpdatedAt())
        .endObject();
    }
    if (!params.isEmpty()) {
      document.startArray(RuleDocument.FIELD_PARAMS);
      for (RuleParamDto param : params) {
        document.startObject()
          .field(RuleDocument.FIELD_PARAM_KEY, param.getName())
          .field(RuleDocument.FIELD_PARAM_TYPE, param.getType())
          .field(RuleDocument.FIELD_PARAM_DEFAULT_VALUE, param.getDefaultValue())
          .field(RuleDocument.FIELD_PARAM_DESCRIPTION, param.getDescription())
          .endObject();
      }
      document.endArray();
    }
    document.endObject();
    return document;
  }

  private XContentBuilder activeRuleDocument(ActiveRuleDto activeRule, Collection<ActiveRuleParamDto> params) throws IOException {
    XContentBuilder document = XContentFactory.jsonBuilder()
      .startObject()
      .field(ActiveRuleDocument.FIELD_ID, activeRule.getId())
      .field(ActiveRuleDocument.FIELD_PARENT_ID, activeRule.getParentId())
      .field(ActiveRuleDocument.FIELD_SEVERITY, Severity.get(activeRule.getSeverity()))
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
}
