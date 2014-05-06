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

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.rules.ActiveRule;
import org.sonar.core.cluster.WorkQueue;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.server.search.BaseIndex;
import org.sonar.server.search.NestedIndex;
import org.sonar.server.search.QueryOptions;

import java.io.IOException;
import java.util.Map;

public class ActiveRuleIndex extends NestedIndex<ActiveRule, ActiveRuleQuery, ActiveRuleDto, ActiveRuleKey> {

  private static final Logger LOG = LoggerFactory.getLogger(ActiveRuleIndex.class);

  public ActiveRuleIndex(ActiveRuleNormalizer normalizer, WorkQueue workQueue, Profiling profiling, BaseIndex<?,?,?,?> index) {
    super(new ActiveRuleIndexDefinition(), normalizer, workQueue, profiling, index);
  }

  @Override
  protected String getParentKeyValue(ActiveRuleKey key) {
    return key.ruleKey().toString();
  }

  @Override
  protected String getIndexField() {
    return RuleNormalizer.RuleField.ACTIVE.key();
  }

  @Override
  protected XContentBuilder getIndexSettings() throws IOException {
    return null;
  }

  @Override
  protected XContentBuilder getMapping() throws IOException {
    return null;
  }

  @Override
  protected QueryBuilder getQuery(ActiveRuleQuery query, QueryOptions options) {
    return null;
  }

  @Override
  protected FilterBuilder getFilter(ActiveRuleQuery query, QueryOptions options) {
    return null;
  }

  @Override
  protected ActiveRule getSearchResult(Map<String, Object> response) {
    return null;
  }

  @Override
  protected SearchRequestBuilder buildRequest(ActiveRuleQuery query, QueryOptions options) {
    return null;
  }
}