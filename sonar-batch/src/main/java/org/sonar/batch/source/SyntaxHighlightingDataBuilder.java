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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Ordering;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.6
 */
public class SyntaxHighlightingDataBuilder {

  private List<SyntaxHighlightingRule> syntaxHighlightingRuleSet;

  public SyntaxHighlightingDataBuilder() {
    syntaxHighlightingRuleSet = newArrayList();
  }

  public SyntaxHighlightingDataBuilder registerHighlightingRule(int startOffset, int endOffset, String typeOfText) {
    if (ruleConflictsWithExistingRules(startOffset, endOffset)) {
      String errorMsg = String.format("Cannot register highlighting rule for characters from %s to %s as it " +
        "overlaps at least one existing rule", startOffset, endOffset);
      LoggerFactory.getLogger(SyntaxHighlightingDataBuilder.class).error(errorMsg);
      throw new UnsupportedOperationException(errorMsg);
    }
    SyntaxHighlightingRule syntaxHighlightingRule = SyntaxHighlightingRule.create(startOffset, endOffset,
      typeOfText);
    this.syntaxHighlightingRuleSet.add(syntaxHighlightingRule);
    return this;
  }

  public SyntaxHighlightingData build() {
    return new SyntaxHighlightingData(getSortedRules());
  }

  private boolean ruleConflictsWithExistingRules(final int startOffset, final int endOffset) {
    Collection<SyntaxHighlightingRule> conflictingRules = Collections2
      .filter(syntaxHighlightingRuleSet, new Predicate<SyntaxHighlightingRule>() {
        @Override
        public boolean apply(@Nullable SyntaxHighlightingRule syntaxHighlightingRule) {

          if (syntaxHighlightingRule != null) {
            boolean overlapsStartBoundary = startOffset < syntaxHighlightingRule.getStartPosition()
              && endOffset >= syntaxHighlightingRule.getStartPosition() + 1
              && endOffset < syntaxHighlightingRule.getEndPosition();

            boolean overlapsEndBoundary = startOffset > syntaxHighlightingRule.getStartPosition()
              && startOffset < syntaxHighlightingRule.getEndPosition()
              && endOffset > syntaxHighlightingRule.getEndPosition();

            return overlapsStartBoundary || overlapsEndBoundary;
          }
          return false;
        }
      });
    return !conflictingRules.isEmpty();
  }

  @VisibleForTesting
  protected List<SyntaxHighlightingRule> getSortedRules() {
    Ordering<SyntaxHighlightingRule> ruleOrdering = new Ordering<SyntaxHighlightingRule>() {
      @Override
      public int compare(@Nullable SyntaxHighlightingRule left,
                         @Nullable SyntaxHighlightingRule right) {
        int result;
        if (left != null && right != null) {
          result = left.getStartPosition() - right.getStartPosition();
          if (result == 0) {
            result = left.getEndPosition() - right.getEndPosition();
          }
          return result;
        }
        return left != null ? 1 : -1;
      }
    };

    return ruleOrdering.immutableSortedCopy(syntaxHighlightingRuleSet);
  }
}
