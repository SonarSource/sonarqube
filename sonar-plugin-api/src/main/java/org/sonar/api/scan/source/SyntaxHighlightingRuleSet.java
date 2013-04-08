/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.scan.source;

import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class SyntaxHighlightingRuleSet {

  private List<SyntaxHighlightingRule> syntaxHighlightingRuleSet;

  private SyntaxHighlightingRuleSet(List<SyntaxHighlightingRule> syntaxHighlightingRules) {
    syntaxHighlightingRuleSet = syntaxHighlightingRules;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private List<SyntaxHighlightingRule> syntaxHighlightingRuleSet;

    public Builder() {
      syntaxHighlightingRuleSet = newArrayList();
    }

    public Builder registerHighlightingRule(int startOffset, int endOffset, String typeOfText) {
      SyntaxHighlightingRule syntaxHighlightingRule = SyntaxHighlightingRule.create(startOffset, endOffset,
              typeOfText);
      this.syntaxHighlightingRuleSet.add(syntaxHighlightingRule);
      return this;
    }

    public SyntaxHighlightingRuleSet build() {
      return new SyntaxHighlightingRuleSet(ImmutableList.copyOf(syntaxHighlightingRuleSet));
    }
  }

  public List<SyntaxHighlightingRule> getSyntaxHighlightingRuleSet() {
    return syntaxHighlightingRuleSet;
  }
}
