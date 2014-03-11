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
package org.sonar.batch.source;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class SyntaxHighlightingDataTest {

  @Test
  public void should_serialize_rules_to_string() throws Exception {

    List<SyntaxHighlightingRule> orderedHighlightingRules = Lists.newArrayList(
      SyntaxHighlightingRule.create(0, 10, "cd"),
      SyntaxHighlightingRule.create(10, 12, "k"),
      SyntaxHighlightingRule.create(12, 20, "cd"),
      SyntaxHighlightingRule.create(24, 38, "k"),
      SyntaxHighlightingRule.create(24, 65, "cppd"),
      SyntaxHighlightingRule.create(42, 50, "k")
    );

    String serializedRules = new SyntaxHighlightingData(orderedHighlightingRules).writeString();
    assertThat(serializedRules).isEqualTo("0,10,cd;10,12,k;12,20,cd;24,38,k;24,65,cppd;42,50,k;");
  }
}
