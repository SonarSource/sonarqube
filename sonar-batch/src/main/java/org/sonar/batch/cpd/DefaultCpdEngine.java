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
package org.sonar.batch.cpd;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.CpdMapping;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DeprecatedDefaultInputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.cpd.index.IndexFactory;
import org.sonar.batch.cpd.index.SonarDuplicationsIndex;
import org.sonar.duplications.DuplicationPredicates;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.internal.pmd.TokenizerBridge;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DefaultCpdEngine extends CpdEngine {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultCpdEngine.class);

  /**
   * Limit of time to analyse one file (in seconds).
   */
  private static final int TIMEOUT = 5 * 60;

  private final IndexFactory indexFactory;
  private final CpdMappings mappings;
  private final FileSystem fs;
  private final Settings settings;
  private final Project project;

  public DefaultCpdEngine(@Nullable Project project, IndexFactory indexFactory, CpdMappings mappings, FileSystem fs, Settings settings) {
    this.project = project;
    this.indexFactory = indexFactory;
    this.mappings = mappings;
    this.fs = fs;
    this.settings = settings;
  }

  public DefaultCpdEngine(IndexFactory indexFactory, CpdMappings mappings, FileSystem fs, Settings settings) {
    this(null, indexFactory, mappings, fs, settings);
  }

  @Override
  public boolean isLanguageSupported(String language) {
    return true;
  }

  @Override
  public void analyse(String languageKey, SensorContext context) {
    CpdMapping mapping = mappings.getMapping(languageKey);
    if (mapping == null) {
      LOG.debug("No CpdMapping for language " + languageKey);
      return;
    }

    String[] cpdExclusions = settings.getStringArray(CoreProperties.CPD_EXCLUSIONS);
    logExclusions(cpdExclusions, LOG);
    FilePredicates p = fs.predicates();
    List<InputFile> sourceFiles = Lists.newArrayList(fs.inputFiles(p.and(
      p.hasType(InputFile.Type.MAIN),
      p.hasLanguage(languageKey),
      p.doesNotMatchPathPatterns(cpdExclusions)
      )));
    if (sourceFiles.isEmpty()) {
      return;
    }

    // Create index
    SonarDuplicationsIndex index = indexFactory.create(project, languageKey);
    populateIndex(languageKey, sourceFiles, mapping, index);

    // Detect
    runCpdAnalysis(languageKey, context, sourceFiles, index);
  }

  private void runCpdAnalysis(String languageKey, SensorContext context, List<InputFile> sourceFiles, SonarDuplicationsIndex index) {
    Predicate<CloneGroup> minimumTokensPredicate = DuplicationPredicates.numberOfUnitsNotLessThan(getMinimumTokens(languageKey));

    ExecutorService executorService = Executors.newSingleThreadExecutor();
    try {
      for (InputFile inputFile : sourceFiles) {
        LOG.debug("Detection of duplications for {}", inputFile);
        String resourceEffectiveKey = ((DeprecatedDefaultInputFile) inputFile).key();
        Collection<Block> fileBlocks = index.getByInputFile(inputFile, resourceEffectiveKey);

        Iterable<CloneGroup> filtered;
        try {
          List<CloneGroup> duplications = executorService.submit(new JavaCpdEngine.Task(index, fileBlocks)).get(TIMEOUT, TimeUnit.SECONDS);
          filtered = Iterables.filter(duplications, minimumTokensPredicate);
        } catch (TimeoutException e) {
          filtered = null;
          LOG.warn("Timeout during detection of duplications for " + inputFile, e);
        } catch (InterruptedException | ExecutionException e) {
          throw new SonarException("Fail during detection of duplication for " + inputFile, e);
        }

        JavaCpdEngine.save(context, inputFile, filtered);
      }
    } finally {
      executorService.shutdown();
    }
  }

  private void populateIndex(String languageKey, List<InputFile> sourceFiles, CpdMapping mapping, SonarDuplicationsIndex index) {
    TokenizerBridge bridge = new TokenizerBridge(mapping.getTokenizer(), fs.encoding().name(), getBlockSize(languageKey));
    for (InputFile inputFile : sourceFiles) {
      LOG.debug("Populating index from {}", inputFile);
      String resourceEffectiveKey = ((DeprecatedDefaultInputFile) inputFile).key();
      List<Block> blocks2 = bridge.chunk(resourceEffectiveKey, inputFile.file());
      index.insert(inputFile, blocks2);
    }
  }

  @VisibleForTesting
  int getBlockSize(String languageKey) {
    int blockSize = settings.getInt("sonar.cpd." + languageKey + ".minimumLines");
    if (blockSize == 0) {
      blockSize = getDefaultBlockSize(languageKey);
    }
    return blockSize;
  }

  @VisibleForTesting
  static int getDefaultBlockSize(String languageKey) {
    if ("cobol".equals(languageKey)) {
      return 30;
    } else if ("abap".equals(languageKey) || "natur".equals(languageKey)) {
      return 20;
    } else {
      return 10;
    }
  }

  @VisibleForTesting
  int getMinimumTokens(String languageKey) {
    int minimumTokens = settings.getInt("sonar.cpd." + languageKey + ".minimumTokens");
    if (minimumTokens == 0) {
      minimumTokens = settings.getInt(CoreProperties.CPD_MINIMUM_TOKENS_PROPERTY);
    }
    if (minimumTokens == 0) {
      minimumTokens = 100;
    }

    return minimumTokens;
  }

}
