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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.io.BytesStream;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.MatchQueryBuilder.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.core.rule.*;
import org.sonar.server.es.ESIndex;
import org.sonar.server.es.SearchQuery;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.paging.PagedResult;
import org.sonar.server.paging.PagingResult;

import javax.annotation.CheckForNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.api.rules.Rule.STATUS_REMOVED;

/**
 * Fill search index with rules
 *
 * @since 4.1
 */
public class RuleRegistry {

  public static final String INDEX_RULES = "rules";
  public static final String TYPE_RULE = "rule";
  private static final String PARAM_NAMEORKEY = "nameOrKey";
  private static final String PARAM_STATUS = "status";

  private final ESIndex searchIndex;
  private final RuleDao ruleDao;

  public RuleRegistry(ESIndex searchIndex, RuleDao ruleDao) {
    this.searchIndex = searchIndex;
    this.ruleDao = ruleDao;
  }

  public void start() {
    searchIndex.addMappingFromClasspath(INDEX_RULES, TYPE_RULE, "/org/sonar/server/es/config/mappings/rule_mapping.json");
  }

  public void bulkRegisterRules(Collection<RuleDto> rules, Multimap<Integer, RuleParamDto> paramsByRule, Multimap<Integer, RuleRuleTagDto> tagsByRule) {
    String[] ids = bulkIndexRules(rules, paramsByRule, tagsByRule);
    removeDeletedRules(ids);
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
      searchQuery.notField(PARAM_STATUS, STATUS_REMOVED);
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

  public PagedResult<Rule> find(RuleQuery query) {
    BoolFilterBuilder mainFilter = FilterBuilders.boolFilter().mustNot(FilterBuilders.termFilter(RuleDocument.FIELD_STATUS, STATUS_REMOVED));
    if (StringUtils.isNotBlank(query.query())) {
      mainFilter.must(FilterBuilders.queryFilter(
        QueryBuilders.matchQuery(RuleDocument.FIELD_NAME+".search", query.query()).operator(Operator.AND)));
    }
    SearchHits hits = searchIndex.executeRequest(
      searchIndex.client().prepareSearch(INDEX_RULES).setTypes(TYPE_RULE)
        .setPostFilter(mainFilter)
        .addSort(RuleDocument.FIELD_NAME, SortOrder.ASC)
        .setSize(query.paging().pageSize())
        .setFrom(query.paging().offset()));

    Builder<Rule> rulesBuilder = ImmutableList.builder();
    for (SearchHit hit: hits.hits()) {
      rulesBuilder.add(RuleDocumentParser.parse(hit.sourceAsMap()));
    }
    return new PagedResult<Rule>(rulesBuilder.build(), PagingResult.create(query.paging().pageSize(), query.paging().pageIndex(), hits.getTotalHits()));
  }

  /**
   * Create or update definition of rule identified by <code>ruleId</code>
   *
   * @param ruleId
   */
  public void saveOrUpdate(int ruleId) {
    RuleDto rule = ruleDao.selectById(ruleId);
    if (rule == null) {
      throw new NotFoundException("Impossible to find rule with ID " + ruleId);
    } else {
      Collection<RuleParamDto> params = ruleDao.selectParameters(rule.getId());
      Collection<RuleRuleTagDto> tags = ruleDao.selectTags(rule.getId());
      save(rule, params, tags);
    }
  }

  public void save(RuleDto rule, Collection<RuleParamDto> params, Collection<RuleRuleTagDto> tags) {
    try {
      searchIndex.putSynchronous(INDEX_RULES, TYPE_RULE, Long.toString(rule.getId()), ruleDocument(rule, params, tags));
    } catch (IOException ioexception) {
      throw new IllegalStateException("Unable to index rule with id=" + rule.getId(), ioexception);
    }
  }

  @CheckForNull
  public Rule findByKey(RuleKey key) {
    final SearchHits hits = searchIndex.executeRequest(searchIndex.client().prepareSearch(INDEX_RULES).setTypes(TYPE_RULE)
      .setPostFilter(FilterBuilders.boolFilter()
        .must(
          FilterBuilders.termFilter(RuleDocument.FIELD_REPOSITORY_KEY, key.repository()),
          FilterBuilders.termFilter(RuleDocument.FIELD_KEY, key.rule())
          )));
    if (hits.totalHits() == 0) {
      return null;
    } else {
      return RuleDocumentParser.parse(hits.hits()[0].sourceAsMap());
    }
  }

  private String[] bulkIndexRules(Collection<RuleDto> rules, Multimap<Integer, RuleParamDto> paramsByRule, Multimap<Integer, RuleRuleTagDto> tagsByRule) {
    try {
      String[] ids = new String[rules.size()];
      BytesStream[] docs = new BytesStream[rules.size()];
      int index = 0;
      TimeProfiler profiler = new TimeProfiler();
      profiler.start("Build rules documents");
      for (RuleDto rule : rules) {
        ids[index] = rule.getId().toString();
        docs[index] = ruleDocument(rule, paramsByRule.get(rule.getId()), tagsByRule.get(rule.getId()));
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

  private XContentBuilder ruleDocument(RuleDto rule, Collection<RuleParamDto> params, Collection<RuleRuleTagDto> tags) throws IOException {
    XContentBuilder document = XContentFactory.jsonBuilder()
      .startObject()
      .field(RuleDocument.FIELD_ID, rule.getId())
      .field(RuleDocument.FIELD_KEY, rule.getRuleKey())
      .field(RuleDocument.FIELD_LANGUAGE, rule.getLanguage())
      .field(RuleDocument.FIELD_NAME, rule.getName())
      .field(RuleDocument.FIELD_DESCRIPTION, rule.getDescription())
      .field(RuleDocument.FIELD_TEMPLATE_ID, rule.getParentId() == null ? null : rule.getParentId())
      .field(RuleDocument.FIELD_REPOSITORY_KEY, rule.getRepositoryKey())
      .field(RuleDocument.FIELD_SEVERITY, rule.getSeverityString())
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

    List<String> systemTags = Lists.newArrayList();
    List<String> adminTags = Lists.newArrayList();
    for (RuleRuleTagDto tag: tags) {
      if (tag.getType() == RuleTagType.SYSTEM) {
        systemTags.add(tag.getTag());
      } else {
        adminTags.add(tag.getTag());
      }
    }
    if (!systemTags.isEmpty()) {
      document.array(RuleDocument.FIELD_SYSTEM_TAGS, systemTags.toArray());
    }
    if (!adminTags.isEmpty()) {
      document.array(RuleDocument.FIELD_ADMIN_TAGS, adminTags.toArray());
    }

    document.endObject();
    return document;
  }
}
