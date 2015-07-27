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
package org.sonar.batch.protocol.input;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

import org.sonar.test.JsonAssert;
import org.assertj.core.util.Lists;
import org.junit.Test;

public class RulesSearchResultTest {
  @Test
  public void testJsonParsing() {
    Rule rule1 = new Rule("squid:S1194", "squid", "S1194", "\"java.lang.Error\" should not be extended", "MAJOR", "java");
    Rule rule2 = new Rule("squid:ObjectFinalizeOverridenCallsSuperFinalizeCheck", "squid", "ObjectFinalizeOverridenCallsSuperFinalizeCheck",
      "super.finalize() should be called at the end of Object.finalize() implementations", "BLOCKER", "java");

    RulesSearchResult rules = new RulesSearchResult();
    rules.setRules(Lists.newArrayList(rule1, rule2));

    JsonAssert
      .assertJson(getClass().getResource("RulesSearchTest/expected.json"))
      .isSimilarTo(rules.toJson());
  }

  @Test
  public void testJsonParsingEmpty() throws IOException {
    URL resource = getClass().getResource("RulesSearchTest/empty.json");
    String json = IOUtils.toString(resource);
    RulesSearchResult result = RulesSearchResult.fromJson(json);

    assertThat(result.getRules()).isEmpty();
  }
}
