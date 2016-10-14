/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.SortedMap;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.batch.sensor.internal.DefaultStorable;
import org.sonar.api.batch.sensor.internal.SensorStorage;

public class DefaultCoverage extends DefaultStorable implements NewCoverage {

  private DefaultInputFile inputFile;
  private CoverageType type;
  private int totalCoveredLines = 0;
  private int totalConditions = 0;
  private int totalCoveredConditions = 0;
  private SortedMap<Integer, Integer> hitsByLine = Maps.newTreeMap();
  private SortedMap<Integer, Integer> conditionsByLine = Maps.newTreeMap();
  private SortedMap<Integer, Integer> coveredConditionsByLine = Maps.newTreeMap();

  public DefaultCoverage() {
    super();
  }

  public DefaultCoverage(@Nullable SensorStorage storage) {
    super(storage);
  }

  @Override
  public DefaultCoverage onFile(InputFile inputFile) {
    this.inputFile = (DefaultInputFile) inputFile;
    return this;
  }

  public InputFile inputFile() {
    return inputFile;
  }

  @Override
  public NewCoverage ofType(CoverageType type) {
    Preconditions.checkNotNull(type, "type can't be null");
    this.type = type;
    return this;
  }

  public CoverageType type() {
    return type;
  }

  @Override
  public NewCoverage lineHits(int line, int hits) {
    validateFile();
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
    Preconditions.checkState(line <= inputFile.lines(), String.format("Line %d is out of range in the file %s (lines: %d)", line, inputFile.relativePath(), inputFile.lines()));
    Preconditions.checkState(line > 0, "Line number must be strictly positive: " + line);
  }

  private void validateFile() {
    Preconditions.checkNotNull(inputFile, "Call onFile() first");
  }

  @Override
  public NewCoverage conditions(int line, int conditions, int coveredConditions) {
    validateFile();
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
    storage.store(this);
  }

}
