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

import com.google.common.collect.Lists;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.batch.cpd.index.SonarDuplicationsIndex;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.report.ReportPublisher;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.BlockChunker;
import org.sonar.duplications.detector.suffixtree.SuffixTreeCloneDetectionAlgorithm;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.CloneIndex;
import org.sonar.duplications.java.JavaStatementBuilder;
import org.sonar.duplications.java.JavaTokenProducer;
import org.sonar.duplications.statement.Statement;
import org.sonar.duplications.statement.StatementChunker;
import org.sonar.duplications.token.TokenChunker;

public class JavaCpdEngine extends AbstractCpdEngine {

  private static final Logger LOG = LoggerFactory.getLogger(JavaCpdEngine.class);

  private static final int BLOCK_SIZE = 10;

  /**
   * Limit of time to analyse one file (in seconds).
   */
  private static final int TIMEOUT = 5 * 60;

  private final FileSystem fs;
  private final Settings settings;
  private final ReportPublisher publisher;
  private final BatchComponentCache batchComponentCache;

  public JavaCpdEngine(FileSystem fs, Settings settings, ReportPublisher publisher, BatchComponentCache batchComponentCache) {
    super(publisher, batchComponentCache);
    this.fs = fs;
    this.settings = settings;
    this.publisher = publisher;
    this.batchComponentCache = batchComponentCache;
  }

  @Override
  public boolean isLanguageSupported(String language) {
    return "java".equals(language);
  }

  @Override
  public void analyse(String languageKey, SensorContext context) {
    String[] cpdExclusions = settings.getStringArray(CoreProperties.CPD_EXCLUSIONS);
    logExclusions(cpdExclusions, LOG);
    FilePredicates p = fs.predicates();
    List<InputFile> sourceFiles = Lists.newArrayList(fs.inputFiles(p.and(
      p.hasType(InputFile.Type.MAIN),
      p.hasLanguage(languageKey),
      p.doesNotMatchPathPatterns(cpdExclusions))));
    if (sourceFiles.isEmpty()) {
      return;
    }
    SonarDuplicationsIndex index = createIndex(sourceFiles);
    detect(index, context, sourceFiles);
  }

  private SonarDuplicationsIndex createIndex(Iterable<InputFile> sourceFiles) {
    final SonarDuplicationsIndex index = new SonarDuplicationsIndex(publisher, batchComponentCache, settings);

    TokenChunker tokenChunker = JavaTokenProducer.build();
    StatementChunker statementChunker = JavaStatementBuilder.build();
    BlockChunker blockChunker = new BlockChunker(BLOCK_SIZE);

    for (InputFile inputFile : sourceFiles) {
      LOG.debug("Populating index from {}", inputFile);
      String resourceEffectiveKey = ((DefaultInputFile) inputFile).key();

      List<Statement> statements;

      Reader reader = null;
      try {
        reader = new InputStreamReader(new FileInputStream(inputFile.file()), fs.encoding());
        statements = statementChunker.chunk(tokenChunker.chunk(reader));
      } catch (FileNotFoundException e) {
        throw new IllegalStateException("Cannot find file " + inputFile.file(), e);
      } finally {
        IOUtils.closeQuietly(reader);
      }

      List<Block> blocks = blockChunker.chunk(resourceEffectiveKey, statements);
      index.insert(inputFile, blocks);
    }

    return index;
  }

  private void detect(SonarDuplicationsIndex index, org.sonar.api.batch.sensor.SensorContext context, List<InputFile> sourceFiles) {
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    try {
      for (InputFile inputFile : sourceFiles) {
        LOG.debug("Detection of duplications for {}", inputFile);
        String resourceEffectiveKey = ((DefaultInputFile) inputFile).key();

        Collection<Block> fileBlocks = index.getByInputFile(inputFile, resourceEffectiveKey);

        List<CloneGroup> clones;
        try {
          clones = executorService.submit(new Task(index, fileBlocks)).get(TIMEOUT, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
          clones = Collections.emptyList();
          LOG.warn("Timeout during detection of duplications for " + inputFile, e);
        } catch (InterruptedException | ExecutionException e) {
          throw new IllegalStateException("Fail during detection of duplication for " + inputFile, e);
        }

        saveDuplications(inputFile, clones);
      }
    } finally {
      executorService.shutdown();
    }
  }

  static class Task implements Callable<List<CloneGroup>> {
    private final CloneIndex index;
    private final Collection<Block> fileBlocks;

    public Task(CloneIndex index, Collection<Block> fileBlocks) {
      this.index = index;
      this.fileBlocks = fileBlocks;
    }

    @Override
    public List<CloneGroup> call() {
      return SuffixTreeCloneDetectionAlgorithm.detect(index, fileBlocks);
    }
  }

}
