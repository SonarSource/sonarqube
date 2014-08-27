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
package org.sonar.batch.highlighting;

import org.sonar.api.batch.sensor.highlighting.TypeOfText;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Ordering;
import org.elasticsearch.common.collect.Sets;

import javax.annotation.Nullable;

import java.util.Iterator;
import java.util.Set;

public class SyntaxHighlightingDataBuilder {

  private Set<SyntaxHighlightingRule> syntaxHighlightingRuleSet;

  public SyntaxHighlightingDataBuilder() {
    syntaxHighlightingRuleSet = Sets.newTreeSet(new Ordering<SyntaxHighlightingRule>() {
      @Override
      public int compare(@Nullable SyntaxHighlightingRule left,
        @Nullable SyntaxHighlightingRule right) {
        int result = left.getStartPosition() - right.getStartPosition();
        if (result == 0) {
          result = left.getEndPosition() - right.getEndPosition();
        }
        return result;
      }
    });
  }

  @VisibleForTesting
  public Set<SyntaxHighlightingRule> getSyntaxHighlightingRuleSet() {
    return syntaxHighlightingRuleSet;
  }

  public SyntaxHighlightingDataBuilder registerHighlightingRule(int startOffset, int endOffset, TypeOfText typeOfText) {
    SyntaxHighlightingRule syntaxHighlightingRule = SyntaxHighlightingRule.create(startOffset, endOffset,
      typeOfText);
    this.syntaxHighlightingRuleSet.add(syntaxHighlightingRule);
    return this;
  }

  public SyntaxHighlightingData build() {
    checkOverlappingBoudaries();
    return new SyntaxHighlightingData(syntaxHighlightingRuleSet);
  }

  private void checkOverlappingBoudaries() {
    if (syntaxHighlightingRuleSet.size() > 1) {
      Iterator<SyntaxHighlightingRule> it = syntaxHighlightingRuleSet.iterator();
      SyntaxHighlightingRule previous = it.next();
      while (it.hasNext()) {
        SyntaxHighlightingRule current = it.next();
        if (previous.getEndPosition() > current.getStartPosition() && !(previous.getEndPosition() >= current.getEndPosition())) {
          String errorMsg = String.format("Cannot register highlighting rule for characters from %s to %s as it " +
            "overlaps at least one existing rule", current.getStartPosition(), current.getEndPosition());
          throw new IllegalStateException(errorMsg);
        }
        previous = current;
      }
    }
  }

}
