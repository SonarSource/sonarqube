/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.internal.DefaultStorable;
import org.sonar.api.batch.sensor.internal.SensorStorage;

import static java.util.Objects.requireNonNull;

public class DefaultHighlighting extends DefaultStorable implements NewHighlighting {

  private final List<SyntaxHighlightingRule> syntaxHighlightingRules;
  private DefaultInputFile inputFile;

  public DefaultHighlighting(SensorStorage storage) {
    super(storage);
    syntaxHighlightingRules = new ArrayList<>();
  }

  public List<SyntaxHighlightingRule> getSyntaxHighlightingRuleSet() {
    return syntaxHighlightingRules;
  }

  private void checkOverlappingBoudaries() {
    if (syntaxHighlightingRules.size() > 1) {
      Iterator<SyntaxHighlightingRule> it = syntaxHighlightingRules.iterator();
      SyntaxHighlightingRule previous = it.next();
      while (it.hasNext()) {
        SyntaxHighlightingRule current = it.next();
        if (previous.range().end().compareTo(current.range().start()) > 0 && (previous.range().end().compareTo(current.range().end()) < 0)) {
          String errorMsg = String.format("Cannot register highlighting rule for characters at %s as it " +
            "overlaps at least one existing rule", current.range());
          throw new IllegalStateException(errorMsg);
        }
        previous = current;
      }
    }
  }

  @Override
  public DefaultHighlighting onFile(InputFile inputFile) {
    requireNonNull(inputFile, "file can't be null");
    this.inputFile = (DefaultInputFile) inputFile;
    return this;
  }

  public InputFile inputFile() {
    return inputFile;
  }

  @Override
  public DefaultHighlighting highlight(int startOffset, int endOffset, TypeOfText typeOfText) {
    checkInputFileNotNull();
    TextRange newRange;
    try {
      newRange = inputFile.newRange(startOffset, endOffset);
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to highlight file " + inputFile, e);
    }
    return highlight(newRange, typeOfText);
  }

  @Override
  public DefaultHighlighting highlight(int startLine, int startLineOffset, int endLine, int endLineOffset, TypeOfText typeOfText) {
    checkInputFileNotNull();
    TextRange newRange;
    try {
      newRange = inputFile.newRange(startLine, startLineOffset, endLine, endLineOffset);
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to highlight file " + inputFile, e);
    }
    return highlight(newRange, typeOfText);
  }

  @Override
  public DefaultHighlighting highlight(TextRange range, TypeOfText typeOfText) {
    SyntaxHighlightingRule syntaxHighlightingRule = SyntaxHighlightingRule.create(range, typeOfText);
    this.syntaxHighlightingRules.add(syntaxHighlightingRule);
    return this;
  }

  @Override
  protected void doSave() {
    checkInputFileNotNull();
    // Sort rules to avoid variation during consecutive runs
    Collections.sort(syntaxHighlightingRules, (left, right) -> {
      int result = left.range().start().compareTo(right.range().start());
      if (result == 0) {
        result = right.range().end().compareTo(left.range().end());
      }
      return result;
    });
    checkOverlappingBoudaries();
    storage.store(this);
  }

  private void checkInputFileNotNull() {
    Preconditions.checkState(inputFile != null, "Call onFile() first");
  }
}
