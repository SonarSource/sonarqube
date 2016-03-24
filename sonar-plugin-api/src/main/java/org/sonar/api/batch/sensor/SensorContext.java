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
package org.sonar.api.batch.sensor;

import com.google.common.annotations.Beta;
import java.io.Serializable;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.Version;

/**
 * See {@link Sensor#execute(SensorContext)}
 * In order to write unit tests you can use {@link SensorContextTester}
 * @since 5.1
 */
@Beta
public interface SensorContext {

  /**
   * Get settings of the current module.
   */
  Settings settings();

  /**
   * Get filesystem of the current module.
   */
  FileSystem fileSystem();

  /**
   * Get list of active rules.
   */
  ActiveRules activeRules();

  /**
   * @since 5.5
   */
  InputModule module();

  /**
   * @since 5.5
   */
  Version getSonarQubeVersion();

  // ----------- MEASURES --------------

  /**
   * Fluent builder to create a new {@link Measure}. Don't forget to call {@link NewMeasure#save()} once all parameters are provided.
   */
  <G extends Serializable> NewMeasure<G> newMeasure();

  // ----------- ISSUES --------------

  /**
   * Fluent builder to create a new {@link Issue}. Don't forget to call {@link NewIssue#save()} once all parameters are provided.
   */
  NewIssue newIssue();

  // ------------ HIGHLIGHTING ------------

  /**
   * Builder to define highlighting of a file. Don't forget to call {@link NewHighlighting#save()} once all elements are provided.
   */
  NewHighlighting newHighlighting();

  // ------------ SYMBOL REFERENCES ------------

  // TODO

  // ------------ TESTS ------------

  /**
   * Builder to define coverage in a file.
   * Don't forget to call {@link NewCoverage#save()}.
   */
  NewCoverage newCoverage();

  // ------------ CPD ------------

  /**
   * Builder to define CPD tokens in a file.
   * Don't forget to call {@link NewCpdTokens#save()}.
   * @since 5.5
   */
  NewCpdTokens newCpdTokens();

}
