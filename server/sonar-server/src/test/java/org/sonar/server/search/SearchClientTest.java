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

public class SearchClientTest {

//  File dataDir;
//  Settings settings;
//
//  @Rule
//  public TemporaryFolder temp = new TemporaryFolder();
//
//  @Before
//  public void createMocks() throws IOException {
//    dataDir = temp.newFolder();
//    settings = new Settings();
//    settings.setProperty("sonar.path.home", dataDir.getAbsolutePath());
//    settings.setProperty(IndexProperties.TYPE, IndexProperties.ES_TYPE.MEMORY.name());
//  }
//
//  @Test(expected = StrictDynamicMappingException.class)
//  public void should_use_default_settings_for_index() throws Exception {
//    SearchClient node = new SearchClient(settings);
//    node.start();
//
//    node.client().admin().indices().prepareCreate("strict")
//      .addMapping("type1", "{\"type1\": {\"properties\": {\"value\": {\"type\": \"string\"}}}}")
//      .execute().actionGet();
//    node.client().admin().cluster().prepareHealth("strict").setWaitForYellowStatus().get(TimeValue.timeValueMillis(1000));
//
//    // strict mapping is enforced
//    try {
//      node.client().prepareIndex("strict", "type1", "666").setSource(
//        XContentFactory.jsonBuilder().startObject().field("unknown", "plouf").endObject()
//      ).get();
//    } finally {
//      node.stop();
//    }
//  }
//
//  @Test
//  public void check_path_analyzer() throws Exception {
//    SearchClient node = new SearchClient(settings);
//    node.start();
//
//    node.client().admin().indices().prepareCreate("path")
//      .addMapping("type1", "{\"type1\": {\"properties\": {\"value\": {\"type\": \"string\"}}}}")
//      .execute().actionGet();
//    node.client().admin().cluster().prepareHealth("path").setWaitForYellowStatus().get(TimeValue.timeValueMillis(1000));
//
//    // default "path_analyzer" analyzer is defined for all indices
//    AnalyzeResponse response = node.client().admin().indices()
//      .prepareAnalyze("path", "/temp/65236/test path/MyFile.java").setAnalyzer("path_analyzer").get();
//    // default "path_analyzer" analyzer is defined for all indices
//    assertThat(response.getTokens()).hasSize(4);
//    assertThat(response.getTokens().get(0).getTerm()).isEqualTo("/temp");
//    assertThat(response.getTokens().get(1).getTerm()).isEqualTo("/temp/65236");
//    assertThat(response.getTokens().get(2).getTerm()).isEqualTo("/temp/65236/test path");
//    assertThat(response.getTokens().get(3).getTerm()).isEqualTo("/temp/65236/test path/MyFile.java");
//
//    node.stop();
//  }
//
//  @Test
//  public void check_sortable_analyzer() throws Exception {
//    SearchClient node = new SearchClient(settings);
//    node.start();
//
//    node.client().admin().indices().prepareCreate("sort")
//      .addMapping("type1", "{\"type1\": {\"properties\": {\"value\": {\"type\": \"string\"}}}}")
//      .execute().actionGet();
//    node.client().admin().cluster().prepareHealth("sort").setWaitForYellowStatus().get(TimeValue.timeValueMillis(1000));
//
//    // default "sortable" analyzer is defined for all indices
//    assertThat(node.client().admin().indices()
//      .prepareAnalyze("sort", "This Is A Wonderful Text").setAnalyzer("sortable").get()
//      .getTokens().get(0).getTerm()).isEqualTo("this is a ");
//
//    node.stop();
//  }
//
//  @Test
//  public void check_gram_analyzer() throws Exception {
//    SearchClient node = new SearchClient(settings);
//    node.start();
//
//    node.client().admin().indices().prepareCreate("gram")
//      .addMapping("type1", "{\"type1\": {\"properties\": {\"value\": {\"type\": \"string\"}}}}")
//      .execute().actionGet();
//    node.client().admin().cluster().prepareHealth("gram").setWaitForYellowStatus().get(TimeValue.timeValueMillis(1000));
//
//    // default "string_gram" analyzer is defined for all indices
//    AnalyzeResponse response = node.client().admin().indices()
//      .prepareAnalyze("gram", "he.llo w@rl#d").setAnalyzer("index_grams").get();
//    assertThat(response.getTokens()).hasSize(10);
//    assertThat(response.getTokens().get(0).getTerm()).isEqualTo("he");
//    assertThat(response.getTokens().get(7).getTerm()).isEqualTo("w@rl");
//
//    node.stop();
//  }
//
//  @Test
//  public void should_fail_to_get_client_if_not_started() {
//    SearchClient node = new SearchClient(settings);
//    try {
//      node.client();
//      fail();
//    } catch (IllegalStateException e) {
//      assertThat(e).hasMessage("Elasticsearch is not started");
//    }
//  }
}
