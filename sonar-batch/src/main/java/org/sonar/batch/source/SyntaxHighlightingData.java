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

import org.sonar.batch.index.Data;

import java.util.List;

public class SyntaxHighlightingData implements Data {

  private static final String FIELD_SEPARATOR = ",";
  private static final String RULE_SEPARATOR = ";";

  private List<SyntaxHighlightingRule> syntaxHighlightingRuleSet;

  public SyntaxHighlightingData(List<SyntaxHighlightingRule> syntaxHighlightingRuleSet) {
    this.syntaxHighlightingRuleSet = syntaxHighlightingRuleSet;
  }

  @Override
  public String writeString() {
    StringBuilder sb = new StringBuilder();
    List<SyntaxHighlightingRule> orderedRules = syntaxHighlightingRuleSet;

    for (SyntaxHighlightingRule highlightingRule : orderedRules) {
      sb.append(highlightingRule.getStartPosition())
        .append(FIELD_SEPARATOR)
        .append(highlightingRule.getEndPosition())
        .append(FIELD_SEPARATOR)
        .append(highlightingRule.getTextType())
        .append(RULE_SEPARATOR);
    }

    return sb.toString();
  }

  @Override
  public void readString(String s) {
    throw new UnsupportedOperationException();
  }
}
