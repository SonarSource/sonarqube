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

import org.apache.commons.beanutils.BeanUtils;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.cluster.WorkQueue;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.rule.RuleConstants;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.es.ESNode;
import org.sonar.server.search.BaseIndex;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Map.Entry;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class RuleIndex extends BaseIndex<RuleKey, RuleDto> {

  private static final Logger LOG = LoggerFactory.getLogger(RuleIndex.class);

  public RuleIndex(WorkQueue queue, RuleDao dao, Profiling profiling, ESNode node) {
    super(queue, dao, profiling, node);
  }

  @Override
  public String getIndexName() {
    return RuleConstants.INDEX_NAME;
  }

  @Override
  protected String getType() {
    return RuleConstants.ES_TYPE;
  }

  protected String getKeyValue(RuleKey key) {
    return key.toString();
  }

  @Override
  protected XContentBuilder getIndexSettings() {
    try {
      return jsonBuilder().startObject()
        .startObject("index")
        .field("number_of_replicas", 0)
        .field("number_of_shards", 3)
        .startObject("mapper")
        .field("dynamic", true)
        .endObject()
        .startObject("analysis")
        .startObject("analyzer")
        .startObject("path_analyzer")
        .field("type", "custom")
        .field("tokenizer", "path_hierarchy")
        .endObject()
        .endObject()
        .endObject()
        .endObject().endObject();
    } catch (IOException e) {
      LOG.error("Could not create index settings for {}", this.getIndexName());
      return null;
    }
  }

  @Override
  protected XContentBuilder getMapping() {
    try {
      return jsonBuilder().startObject()
        .startObject(this.getType())
        .field("dynamic",true)
        .startObject("properties")
        .startObject("id")
        .field("type", "string")
        .field("index", "not_analyzed")
        .endObject()
        .startObject("key")
        .field("type", "string")
        .field("index", "not_analyzed")
        .endObject()
        .startObject("repositoryKey")
        .field("type", "string")
        .field("index", "not_analyzed")
        .endObject()
        .startObject("severity")
        .field("type", "string")
        .field("index", "not_analyzed")
        .endObject()
        .endObject()
        .endObject().endObject();

      // return jsonBuilder().startObject()
      // .startObject("issue")
      // .startObject("properties")
      // .startObject("component.path")
      // .field("type", "string")
      // .field("index_analyzer", "path_analyzer")
      // .field("search_analyzer", "keyword")
      // .endObject()
      // .startObject("rule.name")
      // .field("type", "string")
      // .field("analyzer", "keyword")
      // .endObject()
      // .startObject("root.id")
      // .field("type", "multi_field")
      // .startObject("fields")
      // .startObject("str")
      // .field("type", "string")
      // .field("index","analyzed")
      // .field("analyzer", "default")
      // .endObject()
      // .startObject("num")
      // .field("type", "long")
      // .field("index","analyzed")
      // .endObject()
      // .endObject()
      // .endObject()
      // .endObject().endObject();
    } catch (IOException e) {
      LOG.error("Could not create mapping for {}", this.getIndexName());
      return null;
    }
  }

  @Override
  public XContentBuilder normalize(RuleKey key) {

    RuleDto rule = dao.getByKey(key);

    try {

      XContentBuilder document = jsonBuilder().startObject();

      Map<String, Object> properties = BeanUtils.describe(rule);

      for (Entry<String, Object> property : properties.entrySet()) {
        LOG.trace("NORMALIZING: {} -> {}",property.getKey(), property.getValue());
        document.field(property.getKey(), property.getValue());
      }

      return document.endObject();
    } catch (IOException e) {
      LOG.error("Could not normalize {} in {}", key, this.getClass().getSimpleName());
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      LOG.error("Could not normalize {} in {}", key, this.getClass().getSimpleName());
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      LOG.error("Could not normalize {} in {}", key, this.getClass().getSimpleName());
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      LOG.error("Could not normalize {} in {}", key, this.getClass().getSimpleName());
      e.printStackTrace();
    }
    return null;
  }
}
