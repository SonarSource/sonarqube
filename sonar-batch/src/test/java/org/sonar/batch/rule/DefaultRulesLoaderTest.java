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
package org.sonar.batch.rule;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

import static org.mockito.Matchers.contains;
import static org.assertj.core.api.Assertions.assertThat;
import org.sonar.batch.bootstrap.WSLoader;
import org.junit.Test;
import org.sonar.batch.protocol.input.Rule;
import org.sonar.batch.protocol.input.RulesSearchResult;

public class DefaultRulesLoaderTest {
  @Test
  public void testLoadingJson() throws IOException {
    Rule rule1 = new Rule("squid:S1194", "squid", "S1194", "\"java.lang.Error\" should not be extended");
    Rule rule2 = new Rule("squid:ObjectFinalizeOverridenCallsSuperFinalizeCheck", "squid", "ObjectFinalizeOverridenCallsSuperFinalizeCheck",
      "super.finalize() should be called at the end of Object.finalize() implementations");

    // generate json
    RulesSearchResult rulesSearch = new RulesSearchResult();
    rulesSearch.setRules(Arrays.asList(rule1, rule2));
    String json = rulesSearch.toJson();

    RulesSearchResult empty = new RulesSearchResult();
    empty.setRules(new LinkedList<Rule>());
    String emptyJson = empty.toJson();

    WSLoader wsLoader = mock(WSLoader.class);
    when(wsLoader.loadString(contains("p=1"))).thenReturn(json);
    when(wsLoader.loadString(contains("p=2"))).thenReturn(emptyJson);

    // load
    RulesLoader loader = new DefaultRulesLoader(wsLoader);
    RulesSearchResult rules = loader.load();

    assertThat(rules.toJson()).isEqualTo(json);
  }
}
