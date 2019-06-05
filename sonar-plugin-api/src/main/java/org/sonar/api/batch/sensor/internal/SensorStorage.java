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

import org.sonar.api.batch.sensor.code.NewSignificantCode;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.rule.AdHocRule;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.scanner.ScannerSide;

/**
 * Interface for storing data computed by sensors.
 *
 * @since 5.1
 */
@ScannerSide
public interface SensorStorage {

  void store(Measure measure);

  void store(Issue issue);

  void store(ExternalIssue issue);

  void store(AdHocRule adHocRule);

  void store(NewHighlighting highlighting);

  /**
   * @since 5.2
   */
  void store(NewCoverage defaultCoverage);

  /**
   * @since 5.5
   */
  void store(NewCpdTokens cpdTokens);

  /**
   * @since 5.6
   */
  void store(NewSymbolTable symbolTable);

  /**
   * @since 6.0
   */
  void store(AnalysisError analysisError);

  /**
   * Value is overridden if the key was already stored.
   *
   * @throws IllegalArgumentException if key is null
   * @throws IllegalArgumentException if value is null
   * @since 6.1
   */
  void storeProperty(String key, String value);

  /**
   * @since 7.2
   */
  void store(NewSignificantCode significantCode);
}
