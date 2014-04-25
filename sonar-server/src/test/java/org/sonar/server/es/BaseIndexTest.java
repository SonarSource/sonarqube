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
package org.sonar.server.es;

import java.io.Serializable;
import java.util.Map;

import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.core.profiling.Profiling;
import org.sonar.server.search.BaseIndex;
import static org.fest.assertions.Assertions.assertThat;

@Ignore
public class BaseIndexTest {

  private static final String TEST_NODE_NAME = "es_node_for_tests";

  private BaseIndex<?> searchIndex;
  private Node node;

  @Before
  public void setUp() throws Exception {

    this.node = NodeBuilder.nodeBuilder()
      .settings(ImmutableSettings.settingsBuilder()
        .put("node.name", TEST_NODE_NAME).build())
      .clusterName(BaseIndex.ES_CLUSTER_NAME).node();
  }

  private BaseIndex<?> getBaseIndex(){
    Settings settings = new Settings();
    settings.setProperty("sonar.log.profilingLevel", "BASIC");
    return new BaseIndex<Serializable>(null, new Profiling(settings)) {
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
  public void should_start_and_stop_properly() {

    searchIndex = getBaseIndex();
    searchIndex.start();

    assertThat(node.client().admin().cluster().prepareClusterStats().get().getNodesStats().getCounts().getTotal())
      .isEqualTo(searchIndex.getNodesStats().getCounts().getTotal());

    searchIndex.stop();

  }

  @Test(expected = NoNodeAvailableException.class)
  public void fails_when_es_gone(){
    searchIndex = getBaseIndex();
    searchIndex.start();

    node.stop();


    assertThat(searchIndex.getNodesStats().getCounts().getTotal());

    node.start();


  }
}
