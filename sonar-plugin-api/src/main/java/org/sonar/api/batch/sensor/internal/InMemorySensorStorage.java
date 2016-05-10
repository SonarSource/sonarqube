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
package org.sonar.api.batch.sensor.internal;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.batch.sensor.coverage.internal.DefaultCoverage;
import org.sonar.api.batch.sensor.cpd.internal.DefaultCpdTokens;
import org.sonar.api.batch.sensor.highlighting.internal.DefaultHighlighting;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.symbol.internal.DefaultSymbolTable;

class InMemorySensorStorage implements SensorStorage {

  Table<String, String, Measure> measuresByComponentAndMetric = HashBasedTable.create();

  Collection<Issue> allIssues = new ArrayList<>();

  Map<String, DefaultHighlighting> highlightingByComponent = new HashMap<>();
  Map<String, DefaultCpdTokens> cpdTokensByComponent = new HashMap<>();
  Map<String, Map<CoverageType, DefaultCoverage>> coverageByComponent = new HashMap<>();
  Map<String, DefaultSymbolTable> symbolsPerComponent = new HashMap<>();

  @Override
  public void store(Measure measure) {
    measuresByComponentAndMetric.row(measure.inputComponent().key()).put(measure.metric().key(), measure);
  }

  @Override
  public void store(Issue issue) {
    allIssues.add(issue);
  }

  @Override
  public void store(DefaultHighlighting highlighting) {
    highlightingByComponent.put(highlighting.inputFile().key(), highlighting);
  }

  @Override
  public void store(DefaultCoverage defaultCoverage) {
    String key = defaultCoverage.inputFile().key();
    if (!coverageByComponent.containsKey(key)) {
      coverageByComponent.put(key, new EnumMap<CoverageType, DefaultCoverage>(CoverageType.class));
    }
    coverageByComponent.get(key).put(defaultCoverage.type(), defaultCoverage);
  }

  @Override
  public void store(DefaultCpdTokens defaultCpdTokens) {
    cpdTokensByComponent.put(defaultCpdTokens.inputFile().key(), defaultCpdTokens);
  }

  @Override
  public void store(DefaultSymbolTable symbolTable) {
    symbolsPerComponent.put(symbolTable.inputFile().key(), symbolTable);
  }

}
