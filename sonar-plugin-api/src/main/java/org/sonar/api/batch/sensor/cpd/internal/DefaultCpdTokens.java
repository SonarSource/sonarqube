/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.batch.sensor.cpd.internal;

import java.util.ArrayList;
import java.util.List;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.batch.sensor.internal.DefaultStorable;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.utils.Preconditions.checkState;

public class DefaultCpdTokens extends DefaultStorable implements NewCpdTokens {
  private static final Logger LOG = Loggers.get(DefaultCpdTokens.class);
  private final List<TokensLine> result = new ArrayList<>();
  private DefaultInputFile inputFile;
  private int startLine = Integer.MIN_VALUE;
  private int startIndex = 0;
  private int currentIndex = 0;
  private StringBuilder sb = new StringBuilder();
  private TextRange lastRange;
  private boolean loggedTestCpdWarning = false;

  public DefaultCpdTokens(SensorStorage storage) {
    super(storage);
  }

  @Override
  public DefaultCpdTokens onFile(InputFile inputFile) {
    this.inputFile = (DefaultInputFile) requireNonNull(inputFile, "file can't be null");
    return this;
  }

  public InputFile inputFile() {
    return inputFile;
  }

  @Override
  public NewCpdTokens addToken(int startLine, int startLineOffset, int endLine, int endLineOffset, String image) {
    checkInputFileNotNull();
    TextRange newRange;
    try {
      newRange = inputFile.newRange(startLine, startLineOffset, endLine, endLineOffset);
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to register token in file " + inputFile, e);
    }
    return addToken(newRange, image);
  }

  @Override
  public DefaultCpdTokens addToken(TextRange range, String image) {
    requireNonNull(range, "Range should not be null");
    requireNonNull(image, "Image should not be null");
    checkInputFileNotNull();
    if (isExcludedForDuplication()) {
      return this;
    }
    checkState(lastRange == null || lastRange.end().compareTo(range.start()) <= 0,
      "Tokens of file %s should be provided in order.\nPrevious token: %s\nLast token: %s", inputFile, lastRange, range);

    int line = range.start().line();
    if (line != startLine) {
      addNewTokensLine(result, startIndex, currentIndex, startLine, sb);
      startIndex = currentIndex + 1;
      startLine = line;
    }
    currentIndex++;
    sb.append(image);
    lastRange = range;

    return this;
  }

  private boolean isExcludedForDuplication() {
    if (inputFile.isExcludedForDuplication()) {
      return true;
    }
    if (inputFile.type() == InputFile.Type.TEST) {
      if (!loggedTestCpdWarning) {
        LOG.warn("Duplication reported for '{}' will be ignored because it's a test file.", inputFile);
        loggedTestCpdWarning = true;
      }
      return true;
    }
    return false;
  }

  public List<TokensLine> getTokenLines() {
    return unmodifiableList(new ArrayList<>(result));
  }

  private static void addNewTokensLine(List<TokensLine> result, int startUnit, int endUnit, int startLine, StringBuilder sb) {
    if (sb.length() != 0) {
      result.add(new TokensLine(startUnit, endUnit, startLine, sb.toString()));
      sb.setLength(0);
    }
  }

  @Override
  protected void doSave() {
    checkState(inputFile != null, "Call onFile() first");
    if (isExcludedForDuplication()) {
      return;
    }
    addNewTokensLine(result, startIndex, currentIndex, startLine, sb);
    storage.store(this);
  }

  private void checkInputFileNotNull() {
    checkState(inputFile != null, "Call onFile() first");
  }
}
