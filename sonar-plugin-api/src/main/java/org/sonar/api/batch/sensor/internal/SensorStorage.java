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

import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.batch.sensor.code.internal.DefaultSignificantCode;
import org.sonar.api.batch.sensor.coverage.internal.DefaultCoverage;
import org.sonar.api.batch.sensor.cpd.internal.DefaultCpdTokens;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.highlighting.internal.DefaultHighlighting;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.internal.DefaultExternalIssue;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.rule.internal.DefaultAdHocRule;
import org.sonar.api.batch.sensor.symbol.internal.DefaultSymbolTable;

/**
 * Interface for storing data computed by sensors.
 * @since 5.1
 */
@ScannerSide
public interface SensorStorage {

  void store(Measure measure);

  void store(Issue issue);

  void store(DefaultExternalIssue issue);

  void store(DefaultAdHocRule adHocRule);

  void store(DefaultHighlighting highlighting);

  /**
   * @since 5.2
   */
  void store(DefaultCoverage defaultCoverage);

  /**
   * @since 5.5 
   */
  void store(DefaultCpdTokens defaultCpdTokens);

  /**
   * @since 5.6 
   */
  void store(DefaultSymbolTable symbolTable);

  /**
   * @since 6.0
   */
  void store(AnalysisError analysisError);

  /**
   * Value is overridden if the key was already stored.
   * @throws IllegalArgumentException if key is null
   * @throws IllegalArgumentException if value is null
   * @since 6.1
   */
  void storeProperty(String key, String value);

  /**
   * @since 7.2
   */
  void store(DefaultSignificantCode significantCode);
}
