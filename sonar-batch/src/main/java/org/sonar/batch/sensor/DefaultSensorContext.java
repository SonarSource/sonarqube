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
package org.sonar.batch.sensor;

import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.dependency.NewDependency;
import org.sonar.api.batch.sensor.dependency.internal.DefaultDependency;
import org.sonar.api.batch.sensor.duplication.NewDuplication;
import org.sonar.api.batch.sensor.duplication.internal.DefaultDuplication;
import org.sonar.api.batch.sensor.highlighting.HighlightingBuilder;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.batch.sensor.symbol.SymbolTableBuilder;
import org.sonar.api.config.Settings;
import org.sonar.batch.highlighting.DefaultHighlightingBuilder;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.batch.symbol.DefaultSymbolTableBuilder;

import java.io.Serializable;

public class DefaultSensorContext implements SensorContext {

  private final Settings settings;
  private final FileSystem fs;
  private final ActiveRules activeRules;
  private final ComponentDataCache componentDataCache;
  private final SensorStorage sensorStorage;
  private final AnalysisMode analysisMode;

  public DefaultSensorContext(Settings settings, FileSystem fs, ActiveRules activeRules, AnalysisMode analysisMode, ComponentDataCache componentDataCache,
    SensorStorage sensorStorage) {
    this.settings = settings;
    this.fs = fs;
    this.activeRules = activeRules;
    this.analysisMode = analysisMode;
    this.componentDataCache = componentDataCache;
    this.sensorStorage = sensorStorage;
  }

  @Override
  public Settings settings() {
    return settings;
  }

  @Override
  public FileSystem fileSystem() {
    return fs;
  }

  @Override
  public ActiveRules activeRules() {
    return activeRules;
  }

  @Override
  public AnalysisMode analysisMode() {
    return analysisMode;
  }

  @Override
  public <G extends Serializable> NewMeasure<G> newMeasure() {
    return new DefaultMeasure(sensorStorage);
  }

  @Override
  public NewIssue newIssue() {
    return new DefaultIssue(sensorStorage);
  }

  @Override
  public HighlightingBuilder highlightingBuilder(InputFile inputFile) {
    return new DefaultHighlightingBuilder(((DefaultInputFile) inputFile).key(), componentDataCache);
  }

  @Override
  public SymbolTableBuilder symbolTableBuilder(InputFile inputFile) {
    return new DefaultSymbolTableBuilder(((DefaultInputFile) inputFile).key(), componentDataCache);
  }

  @Override
  public NewDuplication newDuplication() {
    return new DefaultDuplication(sensorStorage);
  }

  @Override
  public NewDependency newDependency() {
    return new DefaultDependency(sensorStorage);
  }

}
