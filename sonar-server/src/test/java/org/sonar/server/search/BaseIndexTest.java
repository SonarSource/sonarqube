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
package org.sonar.server.search;

import com.github.tlrx.elasticsearch.test.annotations.ElasticsearchNode;
import com.github.tlrx.elasticsearch.test.support.junit.runners.ElasticsearchRunner;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.node.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.config.Settings;
import org.sonar.core.cluster.LocalNonBlockingWorkQueue;
import org.sonar.core.profiling.Profiling;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

@Ignore("Should be fixed")
@RunWith(ElasticsearchRunner.class)
public class BaseIndexTest {

  private static final String TEST_NODE_NAME = "es_node_for_tests";

  @ElasticsearchNode(name = TEST_NODE_NAME,
    clusterName = BaseIndex.ES_CLUSTER_NAME,
    local = false, data = true)
  private Node node;

  @Before
  public void setUp() throws Exception {

  }

  private BaseIndex<?> getBaseIndex(){
    LocalNonBlockingWorkQueue queue = new LocalNonBlockingWorkQueue();
    Settings settings = new Settings();
    settings.setProperty("sonar.log.profilingLevel", "BASIC");
    return new BaseIndex<Serializable>(queue, null, new Profiling(settings)) {

      @Override
      public String getIndexName() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      protected org.elasticsearch.common.settings.Settings getIndexSettings() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      protected String getType() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      protected XContentBuilder getMapping() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Collection<Serializable> synchronizeSince(Long date) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      protected QueryBuilder getKeyQuery(Serializable key) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Map<String, Object> normalize(Serializable key) {
        // TODO Auto-generated method stub
        return null;
      }
    };
  }

  @After
  public void tearDown() {
    if (node != null && !node.isClosed()) {
      node.close();
    }
  }

  @Test
  public void baseIndex_connects_to_es() {
    BaseIndex<?> searchIndex = getBaseIndex();
    searchIndex.connect();
    assertThat(node.client().admin().cluster().prepareClusterStats().get().getNodesStats().getCounts().getTotal())
      .isEqualTo(searchIndex.getNodesStats().getCounts().getTotal());

    searchIndex.stop();
  }

  @Test(expected = NoNodeAvailableException.class)
  public void baseIndex_fails_when_es_gone(){
    BaseIndex<?> searchIndex = getBaseIndex();
    searchIndex.connect();
    node.close();
    assertThat(searchIndex.getNodesStats().getCounts().getTotal()).isNotNull();
  }
}
