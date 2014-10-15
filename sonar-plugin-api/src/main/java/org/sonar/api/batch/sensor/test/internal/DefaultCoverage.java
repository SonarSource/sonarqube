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
package org.sonar.api.batch.sensor.test.internal;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.sensor.SensorStorage;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.batch.sensor.test.Coverage;
import org.sonar.api.utils.KeyValueFormat;

import java.util.SortedMap;

public final class DefaultCoverage implements Coverage {

  private static final String INPUT_FILE_SHOULD_BE_NON_NULL = "InputFile should be non null";

  private InputFile file;
  private CoverageType type;
  private int totalCoveredLines = 0, totalConditions = 0, totalCoveredConditions = 0;
  private SortedMap<Integer, Integer> hitsByLine = Maps.newTreeMap();
  private SortedMap<Integer, Integer> conditionsByLine = Maps.newTreeMap();
  private SortedMap<Integer, Integer> coveredConditionsByLine = Maps.newTreeMap();

  protected transient final SensorStorage storage;
  private transient boolean saved = false;

  public DefaultCoverage() {
    this.storage = null;
  }

  public DefaultCoverage(SensorStorage storage) {
    this.storage = storage;
  }

  @Override
  public DefaultCoverage lineHits(int lineId, int hits) {
    if (!hitsByLine.containsKey(lineId)) {
      hitsByLine.put(lineId, hits);
      if (hits > 0) {
        totalCoveredLines += 1;
      }
    }
    return this;
  }

  @Override
  public DefaultCoverage conditions(int lineId, int conditions, int coveredConditions) {
    if (conditions > 0 && !conditionsByLine.containsKey(lineId)) {
      totalConditions += conditions;
      totalCoveredConditions += coveredConditions;
      conditionsByLine.put(lineId, conditions);
      coveredConditionsByLine.put(lineId, coveredConditions);
    }
    return this;
  }

  public InputFile file() {
    return file;
  }

  @Override
  public DefaultCoverage onFile(InputFile inputFile) {
    Preconditions.checkNotNull(inputFile, INPUT_FILE_SHOULD_BE_NON_NULL);
    Preconditions.checkArgument(inputFile.type() == Type.MAIN, "Coverage is only supported on main files");
    this.file = inputFile;
    return this;
  }

  public CoverageType type() {
    return type;
  }

  @Override
  public DefaultCoverage ofType(CoverageType type) {
    Preconditions.checkNotNull(type);
    this.type = type;
    return this;
  }

  public void save() {
    Preconditions.checkNotNull(this.storage, "No persister on this object");
    Preconditions.checkState(!saved, "This object was already saved");
    Preconditions.checkNotNull(this.file, "File is mandatory on Coverage");
    Preconditions.checkNotNull(this.type, "Type is mandatory on Coverage");

    if (hitsByLine.size() > 0) {
      new DefaultMeasure<Integer>(storage)
        .onFile(file)
        .forMetric(type.linesToCover())
        .withValue(hitsByLine.size())
        .save();
      new DefaultMeasure<Integer>(storage)
        .onFile(file)
        .forMetric(type.uncoveredLines())
        .withValue(hitsByLine.size() - totalCoveredLines)
        .save();
      new DefaultMeasure<String>(storage)
        .onFile(file)
        .forMetric(type.lineHitsData())
        .withValue(KeyValueFormat.format(hitsByLine))
        .save();
    }
    if (totalConditions > 0) {
      new DefaultMeasure<Integer>(storage)
        .onFile(file)
        .forMetric(type.conditionsToCover())
        .withValue(totalConditions)
        .save();
      new DefaultMeasure<Integer>(storage)
        .onFile(file)
        .forMetric(type.uncoveredConditions())
        .withValue(totalConditions - totalCoveredConditions)
        .save();
      new DefaultMeasure<String>(storage)
        .onFile(file)
        .forMetric(type.coveredConditionsByLine())
        .withValue(KeyValueFormat.format(coveredConditionsByLine))
        .save();
      new DefaultMeasure<String>(storage)
        .onFile(file)
        .forMetric(type.conditionsByLine())
        .withValue(KeyValueFormat.format(conditionsByLine))
        .save();
    }
  }

}
