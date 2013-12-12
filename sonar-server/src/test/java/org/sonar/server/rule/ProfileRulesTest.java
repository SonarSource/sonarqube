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

import com.github.tlrx.elasticsearch.test.EsSetup;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.client.Requests;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.Paging;
import org.sonar.core.profiling.Profiling;
import org.sonar.server.qualityprofile.QProfile;
import org.sonar.server.qualityprofile.QProfileRule;
import org.sonar.server.search.SearchIndex;
import org.sonar.server.search.SearchNode;
import org.sonar.test.TestUtils;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProfileRulesTest {

  private ProfileRules profileRules;
  private EsSetup esSetup;

  @Before
  public void setUp() {
    esSetup = new EsSetup();
    esSetup.execute(EsSetup.deleteAll());

    SearchNode searchNode = mock(SearchNode.class);
    when(searchNode.client()).thenReturn(esSetup.client());

    SearchIndex index = new SearchIndex(searchNode, new Profiling(new Settings()));
    index.start();
    RuleRegistry registry = new RuleRegistry(index, null, null);
    registry.start();
    profileRules = new ProfileRules(index);
  }

  @After
  public void tearDown() {
    esSetup.terminate();
  }

  @Test
  public void should_find_active_rules() throws Exception {
    esSetup.client().prepareBulk()
      .add(Requests.indexRequest().index("rules").type("rule").source(testFileAsString("should_find_active_rules/rule25.json")))
      .add(Requests.indexRequest().index("rules").type("rule").source(testFileAsString("should_find_active_rules/rule759.json")))
      .add(Requests.indexRequest().index("rules").type("active_rule").parent("25").source(testFileAsString("should_find_active_rules/active_rule25.json")))
      .add(Requests.indexRequest().index("rules").type("active_rule").parent("759").source(testFileAsString("should_find_active_rules/active_rule391.json")))
      .add(Requests.indexRequest().index("rules").type("active_rule").parent("759").source(testFileAsString("should_find_active_rules/active_rule523.json")))
      .setRefresh(true).execute().actionGet();

    Paging paging = Paging.create(10, 1, 10);

    QProfile profile1 = mock(QProfile.class);
    when(profile1.id()).thenReturn(1);
    List<QProfileRule> result1 = profileRules.searchActiveRules(profile1, paging);
    assertThat(result1).hasSize(2);

    QProfile profile2 = mock(QProfile.class);
    when(profile2.id()).thenReturn(2);
    List<QProfileRule> result2 = profileRules.searchActiveRules(profile2, paging);
    assertThat(result2).hasSize(1);
  }

  private String testFileAsString(String testFile) throws Exception {
    return IOUtils.toString(TestUtils.getResource(getClass(), testFile).toURI());
  }
}
