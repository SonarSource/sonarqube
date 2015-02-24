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
package org.sonar.api.batch.sensor.highlighting.internal;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.internal.DefaultStorable;
import org.sonar.api.batch.sensor.internal.SensorStorage;

import javax.annotation.Nullable;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;

public class DefaultHighlighting extends DefaultStorable implements NewHighlighting {

  private InputFile inputFile;
  private Set<SyntaxHighlightingRule> syntaxHighlightingRuleSet;

  public DefaultHighlighting() {
    this(null);
  }

  public DefaultHighlighting(@Nullable SensorStorage storage) {
    super(storage);
    syntaxHighlightingRuleSet = Sets.newTreeSet(new Comparator<SyntaxHighlightingRule>() {
      @Override
      public int compare(SyntaxHighlightingRule left, SyntaxHighlightingRule right) {
        int result = left.getStartPosition() - right.getStartPosition();
        if (result == 0) {
          result = right.getEndPosition() - left.getEndPosition();
        }
        return result;
      }
    });
  }

  public Set<SyntaxHighlightingRule> getSyntaxHighlightingRuleSet() {
    return syntaxHighlightingRuleSet;
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

  @Override
  public DefaultHighlighting onFile(InputFile inputFile) {
    Preconditions.checkNotNull(inputFile, "file can't be null");
    this.inputFile = inputFile;
    return this;
  }

  public InputFile inputFile() {
    return inputFile;
  }

  @Override
  public DefaultHighlighting highlight(int startOffset, int endOffset, TypeOfText typeOfText) {
    Preconditions.checkState(inputFile != null, "Call onFile() first");
    int maxValidOffset = ((DefaultInputFile) inputFile).lastValidOffset();
    checkOffset(startOffset, maxValidOffset, "startOffset");
    checkOffset(endOffset, maxValidOffset, "endOffset");
    Preconditions.checkArgument(startOffset < endOffset, "startOffset (" + startOffset + ") should be < endOffset (" + endOffset + ") for file " + inputFile + ".");
    SyntaxHighlightingRule syntaxHighlightingRule = SyntaxHighlightingRule.create(startOffset, endOffset,
      typeOfText);
    this.syntaxHighlightingRuleSet.add(syntaxHighlightingRule);
    return this;
  }

  private void checkOffset(int offset, int maxValidOffset, String label) {
    Preconditions.checkArgument(offset >= 0 && offset <= maxValidOffset, "Invalid " + label + " " + offset + ". Should be >= 0 and <= " + maxValidOffset
      + " for file " + inputFile);
  }

  @Override
  protected void doSave() {
    Preconditions.checkState(inputFile != null, "Call onFile() first");
    checkOverlappingBoudaries();
    storage.store(this);
  }
}
