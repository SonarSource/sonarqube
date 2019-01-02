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
package org.sonar.api.batch.sensor.coverage.internal;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.batch.sensor.internal.DefaultStorable;
import org.sonar.api.batch.sensor.internal.SensorStorage;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class DefaultCoverage extends DefaultStorable implements NewCoverage {

  private InputFile inputFile;
  private CoverageType type;
  private int totalCoveredLines = 0;
  private int totalConditions = 0;
  private int totalCoveredConditions = 0;
  private SortedMap<Integer, Integer> hitsByLine = new TreeMap<>();
  private SortedMap<Integer, Integer> conditionsByLine = new TreeMap<>();
  private SortedMap<Integer, Integer> coveredConditionsByLine = new TreeMap<>();

  public DefaultCoverage() {
    super();
  }

  public DefaultCoverage(@Nullable SensorStorage storage) {
    super(storage);
  }

  @Override
  public DefaultCoverage onFile(InputFile inputFile) {
    this.inputFile = inputFile;
    return this;
  }

  public InputFile inputFile() {
    return inputFile;
  }

  @Override
  public NewCoverage ofType(CoverageType type) {
    this.type = requireNonNull(type, "type can't be null");
    return this;
  }

  public CoverageType type() {
    return type;
  }

  @Override
  public NewCoverage lineHits(int line, int hits) {
    validateFile();
    if (isExcluded()) {
      return this;
    }
    validateLine(line);

    if (!hitsByLine.containsKey(line)) {
      hitsByLine.put(line, hits);
      if (hits > 0) {
        totalCoveredLines += 1;
      }
    }
    return this;
  }

  private void validateLine(int line) {
    checkState(line <= inputFile.lines(), "Line %s is out of range in the file %s (lines: %s)", line, inputFile, inputFile.lines());
    checkState(line > 0, "Line number must be strictly positive: %s", line);
  }

  private void validateFile() {
    requireNonNull(inputFile, "Call onFile() first");
  }

  @Override
  public NewCoverage conditions(int line, int conditions, int coveredConditions) {
    validateFile();
    if (isExcluded()) {
      return this;
    }
    validateLine(line);

    if (conditions > 0 && !conditionsByLine.containsKey(line)) {
      totalConditions += conditions;
      totalCoveredConditions += coveredConditions;
      conditionsByLine.put(line, conditions);
      coveredConditionsByLine.put(line, coveredConditions);
    }
    return this;
  }

  public int coveredLines() {
    return totalCoveredLines;
  }

  public int linesToCover() {
    return hitsByLine.size();
  }

  public int conditions() {
    return totalConditions;
  }

  public int coveredConditions() {
    return totalCoveredConditions;
  }

  public SortedMap<Integer, Integer> hitsByLine() {
    return Collections.unmodifiableSortedMap(hitsByLine);
  }

  public SortedMap<Integer, Integer> conditionsByLine() {
    return Collections.unmodifiableSortedMap(conditionsByLine);
  }

  public SortedMap<Integer, Integer> coveredConditionsByLine() {
    return Collections.unmodifiableSortedMap(coveredConditionsByLine);
  }

  @Override
  public void doSave() {
    validateFile();
    if (!isExcluded()) {
      storage.store(this);
    }
  }

  private boolean isExcluded() {
    return ((DefaultInputFile) inputFile).isExcludedForCoverage();
  }

}
