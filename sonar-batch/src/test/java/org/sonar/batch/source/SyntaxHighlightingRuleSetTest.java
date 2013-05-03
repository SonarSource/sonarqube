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
package org.sonar.batch.source;


import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class SyntaxHighlightingRuleSetTest {

  private SyntaxHighlightingRuleSet highlightingRules;

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Before
  public void setUpSampleRules() {

    SyntaxHighlightingRuleSet.Builder highlightingRuleSet = SyntaxHighlightingRuleSet.builder();
    highlightingRuleSet.registerHighlightingRule(0, 10, "cd");
    highlightingRuleSet.registerHighlightingRule(10, 12, "k");
    highlightingRuleSet.registerHighlightingRule(24, 38, "k");
    highlightingRuleSet.registerHighlightingRule(42, 50, "k");
    highlightingRuleSet.registerHighlightingRule(24, 65, "cppd");
    highlightingRuleSet.registerHighlightingRule(12, 20, "cd");

    highlightingRules = highlightingRuleSet.build();
  }

  @Test
  public void should_register_highlighting_rule() throws Exception {
    assertThat(highlightingRules.getSyntaxHighlightingRuleSet()).hasSize(6);
  }

  @Test
  public void should_order_by_start_then_end_offset() throws Exception {

    List<SyntaxHighlightingRule> orderedRules = highlightingRules.getOrderedHighlightingRules();

    assertThat(orderedRules).onProperty("startPosition").containsExactly(0, 10, 12, 24, 24, 42);
    assertThat(orderedRules).onProperty("endPosition").containsExactly(10, 12, 20, 38, 65, 50);
    assertThat(orderedRules).onProperty("textType").containsExactly("cd", "k", "cd", "k", "cppd", "k");
  }

  @Test
  public void should_serialize_rules_to_string() throws Exception {

    String serializedRules = highlightingRules.writeString();
    assertThat(serializedRules).isEqualTo("0,10,cd;10,12,k;12,20,cd;24,38,k;24,65,cppd;42,50,k;");
  }

  @Test
  public void should_prevent_rules_overlapping() throws Exception {

    throwable.expect(UnsupportedOperationException.class);

    SyntaxHighlightingRuleSet.Builder builder = SyntaxHighlightingRuleSet.builder();
    builder.registerHighlightingRule(0, 10, "k");
    builder.registerHighlightingRule(8, 15, "k");
  }
}
