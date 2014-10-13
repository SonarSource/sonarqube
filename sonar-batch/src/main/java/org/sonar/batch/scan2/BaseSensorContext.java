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
package org.sonar.batch.scan2;

import com.google.common.base.Preconditions;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorStorage;
import org.sonar.api.batch.sensor.dependency.Dependency;
import org.sonar.api.batch.sensor.dependency.internal.DefaultDependency;
import org.sonar.api.batch.sensor.duplication.DuplicationBuilder;
import org.sonar.api.batch.sensor.duplication.DuplicationGroup;
import org.sonar.api.batch.sensor.duplication.DuplicationTokenBuilder;
import org.sonar.api.batch.sensor.duplication.internal.DefaultDuplicationBuilder;
import org.sonar.api.batch.sensor.highlighting.HighlightingBuilder;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.batch.sensor.symbol.SymbolTableBuilder;
import org.sonar.api.batch.sensor.test.TestCaseCoverage;
import org.sonar.api.batch.sensor.test.TestCaseExecution;
import org.sonar.api.batch.sensor.test.internal.DefaultTestCaseCoverage;
import org.sonar.api.batch.sensor.test.internal.DefaultTestCaseExecution;
import org.sonar.api.config.Settings;
import org.sonar.batch.duplication.BlockCache;
import org.sonar.batch.duplication.DefaultTokenBuilder;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.batch.highlighting.DefaultHighlightingBuilder;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.batch.scan.SensorContextAdapter;
import org.sonar.batch.symbol.DefaultSymbolTableBuilder;
import org.sonar.duplications.internal.pmd.PmdBlockChunker;

import java.io.Serializable;
import java.util.List;

/**
 * Common bits between {@link DefaultSensorContext} and {@link SensorContextAdapter}
 * @author julien
 *
 */
public abstract class BaseSensorContext implements SensorContext, SensorStorage {

  private final Settings settings;
  private final FileSystem fs;
  private final ActiveRules activeRules;
  private final ComponentDataCache componentDataCache;
  private final BlockCache blockCache;
  private final DuplicationCache duplicationCache;

  protected BaseSensorContext(Settings settings, FileSystem fs, ActiveRules activeRules, ComponentDataCache componentDataCache,
    BlockCache blockCache, DuplicationCache duplicationCache) {
    this.settings = settings;
    this.fs = fs;
    this.activeRules = activeRules;
    this.componentDataCache = componentDataCache;
    this.blockCache = blockCache;
    this.duplicationCache = duplicationCache;
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
  public <G extends Serializable> Measure<G> newMeasure() {
    return (Measure<G>) new DefaultMeasure(this);
  }

  @Override
  public Issue newIssue() {
    return new DefaultIssue(this);
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
  public DuplicationTokenBuilder duplicationTokenBuilder(InputFile inputFile) {
    PmdBlockChunker blockChunker = new PmdBlockChunker(getBlockSize(inputFile.language()));

    return new DefaultTokenBuilder(inputFile, blockCache, blockChunker);
  }

  @Override
  public DuplicationBuilder duplicationBuilder(InputFile inputFile) {
    return new DefaultDuplicationBuilder(inputFile);
  }

  @Override
  public void saveDuplications(InputFile inputFile, List<DuplicationGroup> duplications) {
    Preconditions.checkState(!duplications.isEmpty(), "Empty duplications");
    String effectiveKey = ((DefaultInputFile) inputFile).key();
    for (DuplicationGroup duplicationGroup : duplications) {
      Preconditions.checkState(effectiveKey.equals(duplicationGroup.originBlock().resourceKey()), "Invalid duplication group");
    }
    duplicationCache.put(effectiveKey, duplications);
  }

  private int getBlockSize(String languageKey) {
    int blockSize = settings.getInt("sonar.cpd." + languageKey + ".minimumLines");
    if (blockSize == 0) {
      blockSize = getDefaultBlockSize(languageKey);
    }
    return blockSize;
  }

  private static int getDefaultBlockSize(String languageKey) {
    if ("cobol".equals(languageKey)) {
      return 30;
    } else if ("abap".equals(languageKey) || "natur".equals(languageKey)) {
      return 20;
    } else {
      return 10;
    }
  }

  @Override
  public TestCaseExecution newTestCaseExecution() {
    return new DefaultTestCaseExecution(this);
  }

  @Override
  public TestCaseCoverage newTestCaseCoverage() {
    return new DefaultTestCaseCoverage(this);
  }

  @Override
  public Dependency newDependency() {
    return new DefaultDependency(this);
  }

}
