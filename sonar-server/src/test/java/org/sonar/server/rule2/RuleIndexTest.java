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
//package org.sonar.server.rule2;
//
//import com.github.tlrx.elasticsearch.test.annotations.ElasticsearchNode;
//import com.github.tlrx.elasticsearch.test.support.junit.runners.ElasticsearchRunner;
//import org.elasticsearch.node.Node;
//import org.junit.Before;
//import org.junit.Ignore;
//import org.junit.runner.RunWith;
//import org.sonar.server.es.ESNode;
//import org.sonar.server.search.BaseIndex;
//
//@RunWith(ElasticsearchRunner.class)
//@Ignore("Same problem as with BaseIndex test")
//public class RuleIndexTest {
//
//  private static final String TEST_NODE_NAME = "es_node_for_tests";
//
//  @ElasticsearchNode(name = TEST_NODE_NAME,
//      clusterName = BaseIndex.ES_CLUSTER_NAME,
//      local = true, data = true)
//  private Node node;
//
//  private ESNode esNode;
//
//  @Before
//  public void setUp() throws Exception {
//    esNode = new ESNode(fileSystem, settings)
//  }
////
////  private RuleIndex getRuleIndex(){
////    LocalNonBlockingWorkQueue queue = new LocalNonBlockingWorkQueue();
////    Settings settings = new Settings();
////    settings.setProperty("sonar.log.profilingLevel", "BASIC");
////    RuleIndex rindex =  new RuleIndex(queue, null, new Profiling(settings));
////    return rindex;
////  }
////
////  @After
////  public void tearDown() {
////    if (node != null && !node.isClosed()) {
////      node.close();
////    }
////  }
////
////  @Test
////  public void test_ruleIndex_conencts_to_es() {
////
////    RuleIndex ruleIndex = getRuleIndex();
////    ruleIndex.connect();
////
////    assertThat(node.client().admin().cluster().prepareClusterStats().get().getNodesStats().getCounts().getTotal())
////      .isEqualTo(ruleIndex.getNodesStats().getCounts().getTotal());
////
////    ruleIndex.stop();
////
////  }
//}
