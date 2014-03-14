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
package org.sonar.plugins.cpd;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.CpdMapping;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.duplications.DuplicationPredicates;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.internal.pmd.TokenizerBridge;
import org.sonar.plugins.cpd.index.IndexFactory;
import org.sonar.plugins.cpd.index.SonarDuplicationsIndex;

import javax.annotation.CheckForNull;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class SonarBridgeEngine extends CpdEngine {

  private static final Logger LOG = LoggerFactory.getLogger(SonarBridgeEngine.class);

  /**
   * Limit of time to analyse one file (in seconds).
   */
  private static final int TIMEOUT = 5 * 60;

  private final IndexFactory indexFactory;
  private final CpdMapping[] mappings;
  private final FileSystem fs;
  private final Settings settings;

  public SonarBridgeEngine(IndexFactory indexFactory, CpdMapping[] mappings, FileSystem fs, Settings settings) {
    this.indexFactory = indexFactory;
    this.mappings = mappings;
    this.fs = fs;
    this.settings = settings;
  }

  public SonarBridgeEngine(IndexFactory indexFactory, FileSystem fs, Settings settings) {
    this(indexFactory, new CpdMapping[0], fs, settings);
  }

  @Override
  public boolean isLanguageSupported(String language) {
    return getMapping(language) != null;
  }

  @Override
  public void analyse(Project project, String languageKey, SensorContext context) {
    String[] cpdExclusions = settings.getStringArray(CoreProperties.CPD_EXCLUSIONS);
    logExclusions(cpdExclusions, LOG);
    List<InputFile> sourceFiles = Lists.newArrayList(fs.inputFiles(fs.predicates().and(
      fs.predicates().hasType(InputFile.Type.MAIN),
      fs.predicates().hasLanguage(languageKey),
      fs.predicates().doesNotMatchPathPatterns(cpdExclusions)
    )));
    if (sourceFiles.isEmpty()) {
      return;
    }

    CpdMapping mapping = getMapping(languageKey);
    if (mapping == null) {
      return;
    }

    // Create index
    SonarDuplicationsIndex index = indexFactory.create(project);

    TokenizerBridge bridge = new TokenizerBridge(mapping.getTokenizer(), fs.encoding().name(), getBlockSize(project, languageKey));
    for (InputFile inputFile : sourceFiles) {
      LOG.debug("Populating index from {}", inputFile);
      String resourceEffectiveKey = ((DefaultInputFile) inputFile).key();
      List<Block> blocks = bridge.chunk(resourceEffectiveKey, inputFile.file());
      index.insert(inputFile, blocks);
    }

    // Detect
    Predicate<CloneGroup> minimumTokensPredicate = DuplicationPredicates.numberOfUnitsNotLessThan(getMinimumTokens(project, languageKey));

    ExecutorService executorService = Executors.newSingleThreadExecutor();
    try {
      for (InputFile inputFile : sourceFiles) {
        LOG.debug("Detection of duplications for {}", inputFile);
        String resourceEffectiveKey = ((DefaultInputFile) inputFile).key();
        Collection<Block> fileBlocks = index.getByInputFile(inputFile, resourceEffectiveKey);

        Iterable<CloneGroup> filtered;
        try {
          List<CloneGroup> duplications = executorService.submit(new SonarEngine.Task(index, fileBlocks)).get(TIMEOUT, TimeUnit.SECONDS);
          filtered = Iterables.filter(duplications, minimumTokensPredicate);
        } catch (TimeoutException e) {
          filtered = null;
          LOG.warn("Timeout during detection of duplications for " + inputFile, e);
        } catch (InterruptedException e) {
          throw new SonarException("Fail during detection of duplication for " + inputFile, e);
        } catch (ExecutionException e) {
          throw new SonarException("Fail during detection of duplication for " + inputFile, e);
        }

        SonarEngine.save(context, inputFile, filtered);
      }
    } finally {
      executorService.shutdown();
    }
  }

  @VisibleForTesting
  int getBlockSize(Project project, String languageKey) {
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
  int getMinimumTokens(Project project, String languageKey) {
    int minimumTokens = settings.getInt("sonar.cpd." + languageKey + ".minimumTokens");
    if (minimumTokens == 0) {
      minimumTokens = settings.getInt(CoreProperties.CPD_MINIMUM_TOKENS_PROPERTY);
    }
    if (minimumTokens == 0) {
      minimumTokens = CoreProperties.CPD_MINIMUM_TOKENS_DEFAULT_VALUE;
    }

    return minimumTokens;
  }

  @CheckForNull
  private CpdMapping getMapping(String language) {
    if (mappings != null) {
      for (CpdMapping cpdMapping : mappings) {
        if (cpdMapping.getLanguage().getKey().equals(language)) {
          return cpdMapping;
        }
      }
    }
    return null;
  }

}
