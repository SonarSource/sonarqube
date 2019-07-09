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
package org.sonar.api.batch.sensor.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.api.batch.sensor.code.NewSignificantCode;
import org.sonar.api.batch.sensor.code.internal.DefaultSignificantCode;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.batch.sensor.coverage.internal.DefaultCoverage;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.batch.sensor.cpd.internal.DefaultCpdTokens;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.internal.DefaultHighlighting;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.rule.AdHocRule;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.batch.sensor.symbol.internal.DefaultSymbolTable;

import static org.sonar.api.utils.Preconditions.checkArgument;

class InMemorySensorStorage implements SensorStorage {

  Map<String, Map<String, Measure>> measuresByComponentAndMetric = new HashMap<>();

  Collection<Issue> allIssues = new ArrayList<>();
  Collection<ExternalIssue> allExternalIssues = new ArrayList<>();
  Collection<AdHocRule> allAdHocRules = new ArrayList<>();
  Collection<AnalysisError> allAnalysisErrors = new ArrayList<>();

  Map<String, NewHighlighting> highlightingByComponent = new HashMap<>();
  Map<String, DefaultCpdTokens> cpdTokensByComponent = new HashMap<>();
  Map<String, List<DefaultCoverage>> coverageByComponent = new HashMap<>();
  Map<String, DefaultSymbolTable> symbolsPerComponent = new HashMap<>();
  Map<String, String> contextProperties = new HashMap<>();
  Map<String, DefaultSignificantCode> significantCodePerComponent = new HashMap<>();

  @Override
  public void store(Measure measure) {
    // Emulate duplicate measure check
    String componentKey = measure.inputComponent().key();
    String metricKey = measure.metric().key();
    if (measuresByComponentAndMetric.getOrDefault(componentKey, Collections.emptyMap()).containsKey(metricKey)) {
      throw new IllegalStateException("Can not add the same measure twice");
    }
    measuresByComponentAndMetric.computeIfAbsent(componentKey, x -> new HashMap<>()).put(metricKey, measure);
  }

  @Override
  public void store(Issue issue) {
    allIssues.add(issue);
  }

  @Override
  public void store(AdHocRule adHocRule) {
    allAdHocRules.add(adHocRule);
  }

  @Override
  public void store(NewHighlighting newHighlighting) {
    DefaultHighlighting highlighting = (DefaultHighlighting) newHighlighting;
    String fileKey = highlighting.inputFile().key();
    // Emulate duplicate storage check
    if (highlightingByComponent.containsKey(fileKey)) {
      throw new UnsupportedOperationException("Trying to save highlighting twice for the same file is not supported: " + highlighting.inputFile());
    }
    highlightingByComponent.put(fileKey, highlighting);
  }

  @Override
  public void store(NewCoverage coverage) {
    DefaultCoverage defaultCoverage = (DefaultCoverage) coverage;
    String fileKey = defaultCoverage.inputFile().key();
    coverageByComponent.computeIfAbsent(fileKey, x -> new ArrayList<>()).add(defaultCoverage);
  }

  @Override
  public void store(NewCpdTokens cpdTokens) {
    DefaultCpdTokens defaultCpdTokens = (DefaultCpdTokens) cpdTokens;
    String fileKey = defaultCpdTokens.inputFile().key();
    // Emulate duplicate storage check
    if (cpdTokensByComponent.containsKey(fileKey)) {
      throw new UnsupportedOperationException("Trying to save CPD tokens twice for the same file is not supported: " + defaultCpdTokens.inputFile());
    }
    cpdTokensByComponent.put(fileKey, defaultCpdTokens);
  }

  @Override
  public void store(NewSymbolTable newSymbolTable) {
    DefaultSymbolTable symbolTable = (DefaultSymbolTable) newSymbolTable;
    String fileKey = symbolTable.inputFile().key();
    // Emulate duplicate storage check
    if (symbolsPerComponent.containsKey(fileKey)) {
      throw new UnsupportedOperationException("Trying to save symbol table twice for the same file is not supported: " + symbolTable.inputFile());
    }
    symbolsPerComponent.put(fileKey, symbolTable);
  }

  @Override
  public void store(AnalysisError analysisError) {
    allAnalysisErrors.add(analysisError);
  }

  @Override
  public void storeProperty(String key, String value) {
    checkArgument(key != null, "Key of context property must not be null");
    checkArgument(value != null, "Value of context property must not be null");
    contextProperties.put(key, value);
  }

  @Override
  public void store(ExternalIssue issue) {
    allExternalIssues.add(issue);
  }

  @Override
  public void store(NewSignificantCode newSignificantCode) {
    DefaultSignificantCode significantCode = (DefaultSignificantCode) newSignificantCode;
    String fileKey = significantCode.inputFile().key();
    // Emulate duplicate storage check
    if (significantCodePerComponent.containsKey(fileKey)) {
      throw new UnsupportedOperationException("Trying to save significant code information twice for the same file is not supported: " + significantCode.inputFile());
    }
    significantCodePerComponent.put(fileKey, significantCode);
  }
}
