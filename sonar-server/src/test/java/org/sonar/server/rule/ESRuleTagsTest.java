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
package org.sonar.server.rule;

import com.github.tlrx.elasticsearch.test.EsSetup;
import com.github.tlrx.elasticsearch.test.request.Index;
import com.google.common.collect.ImmutableList;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.config.Settings;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.rule.RuleTagDto;
import org.sonar.server.es.ESIndex;
import org.sonar.server.es.ESNode;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ESRuleTagsTest {

  EsSetup esSetup;

  ESIndex searchIndex;

  ESRuleTags ruleTags;

  @Before
  public void setUp() throws Exception {
    esSetup = new EsSetup(ImmutableSettings.builder().loadFromUrl(ESNode.class.getResource("config/elasticsearch.json")).build());
    esSetup.execute(EsSetup.deleteAll());

    ESNode node = mock(ESNode.class);
    when(node.client()).thenReturn(esSetup.client());

    Settings settings = new Settings();
    settings.setProperty("sonar.log.profilingLevel", "FULL");
    Profiling profiling = new Profiling(settings);
    searchIndex = new ESIndex(node, profiling);
    searchIndex.start();

    ruleTags = new ESRuleTags(searchIndex);
    ruleTags.start();
  }

  @After
  public void tearDown() {
    searchIndex.stop();
    esSetup.terminate();
  }

  @Test
  public void should_register_mapping_at_startup() {
    assertThat(esSetup.exists("rules")).isTrue();
    assertThat(esSetup.client().admin().indices().prepareTypesExists("rules").setTypes("tag").execute().actionGet().isExists()).isTrue();
  }

  @Test
  public void should_skip_bulk_if_no_tag() {
    esSetup.execute(
      indexTagDocument("tag5"),
      indexTagDocument("tag6"),
      indexTagDocument("tag7"));
    ruleTags.putAllTags(ImmutableList.<RuleTagDto>of());
    checkTagCount(0L);
  }

  @Test
  public void should_register_new_tags() throws Exception {
    ruleTags.putAllTags(ImmutableList.of(
      newRuleTagDto("tag1"),
      newRuleTagDto("tag2"),
      newRuleTagDto("tag3"),
      newRuleTagDto("tag4"),
      newRuleTagDto("tag5")
    ));

    checkTagCount(5L);
  }


  @Test
  public void should_remove_unused_tags() throws Exception {

    esSetup.execute(
      indexTagDocument("tag4"),
      indexTagDocument("tag5"),
      indexTagDocument("tag6"),
      indexTagDocument("tag7"));

    ruleTags.putAllTags(ImmutableList.of(
      newRuleTagDto("tag1"),
      newRuleTagDto("tag2"),
      newRuleTagDto("tag3"),
      newRuleTagDto("tag4"),
      newRuleTagDto("tag5")
    ));

    checkTagCount(5L);
  }

  @Test
  public void should_delete_tags() throws Exception {

    esSetup.execute(
      indexTagDocument("tag1"),
      indexTagDocument("tag2"),
      indexTagDocument("tag3"),
      indexTagDocument("tag4"));

    ruleTags.delete("tag1", "tag2");

    checkTagCount(2L);
  }

  private void checkTagCount(long count) {
    assertThat(ruleTags.searchAllTags()).hasSize((int) count);
  }

  private RuleTagDto newRuleTagDto(String tagValue) {
    return new RuleTagDto().setTag(tagValue);
  }

  private Index indexTagDocument(String tagValue) {
    return EsSetup.index(RuleRegistry.INDEX_RULES, ESRuleTags.TYPE_TAG).withSource(String.format("{\"value\":\"%s\"}", tagValue));
  }
}
