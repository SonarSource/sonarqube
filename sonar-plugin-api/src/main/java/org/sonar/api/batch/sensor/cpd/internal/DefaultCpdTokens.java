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
package org.sonar.api.batch.sensor.cpd.internal;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.PathPattern;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.batch.sensor.internal.DefaultStorable;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.config.Configuration;
import org.sonar.duplications.internal.pmd.TokensLine;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

public class DefaultCpdTokens extends DefaultStorable implements NewCpdTokens {

  private final Configuration config;
  private final ArrayList<TokensLine> result = new ArrayList<>();
  private InputFile inputFile;
  private int startLine = Integer.MIN_VALUE;
  private int startIndex = 0;
  private int currentIndex = 0;
  private StringBuilder sb = new StringBuilder();
  private TextRange lastRange;
  private boolean excluded;

  public DefaultCpdTokens(Configuration config, SensorStorage storage) {
    super(storage);
    this.config = config;
  }

  @Override
  public DefaultCpdTokens onFile(InputFile inputFile) {
    this.inputFile = requireNonNull(inputFile, "file can't be null");
    String[] cpdExclusions = config.getStringArray(CoreProperties.CPD_EXCLUSIONS);
    for (PathPattern cpdExclusion : PathPattern.create(cpdExclusions)) {
      if (cpdExclusion.match(inputFile.path(), Paths.get(inputFile.relativePath()))) {
        this.excluded = true;
      }
    }
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
    if (excluded) {
      return this;
    }
    checkState(lastRange == null || lastRange.end().compareTo(range.start()) <= 0,
      "Tokens of file %s should be provided in order.\nPrevious token: %s\nLast token: %s", inputFile, lastRange, range);

    String value = image;

    int line = range.start().line();
    if (line != startLine) {
      addNewTokensLine(result, startIndex, currentIndex, startLine, sb);
      startIndex = currentIndex + 1;
      startLine = line;
    }
    currentIndex++;
    sb.append(value);
    lastRange = range;

    return this;
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
    if (excluded) {
      return;
    }
    addNewTokensLine(result, startIndex, currentIndex, startLine, sb);
    storage.store(this);
  }

  private void checkInputFileNotNull() {
    checkState(inputFile != null, "Call onFile() first");
  }
}
