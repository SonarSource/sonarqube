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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DeprecatedDefaultInputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.duplication.DuplicationBuilder;
import org.sonar.api.batch.sensor.duplication.internal.DefaultDuplicationBuilder;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.BlockChunker;
import org.sonar.duplications.detector.suffixtree.SuffixTreeCloneDetectionAlgorithm;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.CloneIndex;
import org.sonar.duplications.index.ClonePart;
import org.sonar.duplications.java.JavaStatementBuilder;
import org.sonar.duplications.java.JavaTokenProducer;
import org.sonar.duplications.statement.Statement;
import org.sonar.duplications.statement.StatementChunker;
import org.sonar.duplications.token.TokenChunker;
import org.sonar.plugins.cpd.index.IndexFactory;
import org.sonar.plugins.cpd.index.SonarDuplicationsIndex;

import javax.annotation.Nullable;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class JavaCpdEngine extends CpdEngine {

  private static final Logger LOG = LoggerFactory.getLogger(JavaCpdEngine.class);

  private static final int BLOCK_SIZE = 10;

  /**
   * Limit of time to analyse one file (in seconds).
   */
  private static final int TIMEOUT = 5 * 60;

  private final IndexFactory indexFactory;
  private final FileSystem fs;
  private final Settings settings;
  private final Project project;
  private final FileLinesContextFactory contextFactory;

  public JavaCpdEngine(@Nullable Project project, IndexFactory indexFactory, FileSystem fs, Settings settings, FileLinesContextFactory contextFactory) {
    this.project = project;
    this.indexFactory = indexFactory;
    this.fs = fs;
    this.settings = settings;
    this.contextFactory = contextFactory;
  }

  public JavaCpdEngine(IndexFactory indexFactory, FileSystem fs, Settings settings, FileLinesContextFactory contextFactory) {
    this(null, indexFactory, fs, settings, contextFactory);
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
      p.doesNotMatchPathPatterns(cpdExclusions)
      )));
    if (sourceFiles.isEmpty()) {
      return;
    }
    SonarDuplicationsIndex index = createIndex(project, languageKey, sourceFiles);
    detect(index, context, sourceFiles);
  }

  private SonarDuplicationsIndex createIndex(@Nullable Project project, String language, Iterable<InputFile> sourceFiles) {
    final SonarDuplicationsIndex index = indexFactory.create(project, language);

    TokenChunker tokenChunker = JavaTokenProducer.build();
    StatementChunker statementChunker = JavaStatementBuilder.build();
    BlockChunker blockChunker = new BlockChunker(BLOCK_SIZE);

    for (InputFile inputFile : sourceFiles) {
      LOG.debug("Populating index from {}", inputFile);
      String resourceEffectiveKey = ((DeprecatedDefaultInputFile) inputFile).key();

      List<Statement> statements;

      Reader reader = null;
      try {
        reader = new InputStreamReader(new FileInputStream(inputFile.file()), fs.encoding());
        statements = statementChunker.chunk(tokenChunker.chunk(reader));
      } catch (FileNotFoundException e) {
        throw new SonarException("Cannot find file " + inputFile.file(), e);
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
        String resourceEffectiveKey = ((DeprecatedDefaultInputFile) inputFile).key();

        Collection<Block> fileBlocks = index.getByInputFile(inputFile, resourceEffectiveKey);

        List<CloneGroup> clones;
        try {
          clones = executorService.submit(new Task(index, fileBlocks)).get(TIMEOUT, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
          clones = null;
          LOG.warn("Timeout during detection of duplications for " + inputFile, e);
        } catch (InterruptedException e) {
          throw new SonarException("Fail during detection of duplication for " + inputFile, e);
        } catch (ExecutionException e) {
          throw new SonarException("Fail during detection of duplication for " + inputFile, e);
        }

        save(context, inputFile, clones, contextFactory);
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

    public List<CloneGroup> call() {
      return SuffixTreeCloneDetectionAlgorithm.detect(index, fileBlocks);
    }
  }

  static void save(org.sonar.api.batch.sensor.SensorContext context, InputFile inputFile, @Nullable Iterable<CloneGroup> duplications,
    FileLinesContextFactory contextFactory) {
    if (duplications == null || Iterables.isEmpty(duplications)) {
      return;
    }
    Set<Integer> duplicatedLines = new HashSet<Integer>();
    int duplicatedBlocks = computeBlockAndLineCount(duplications, duplicatedLines);
    FileLinesContext linesContext = contextFactory.createFor(inputFile);
    for (int i = 1; i <= inputFile.lines(); i++) {
      linesContext.setIntValue(CoreMetrics.DUPLICATION_LINES_DATA_KEY, i, duplicatedLines.contains(i) ? 1 : 0);
    }
    linesContext.save();
    // Save
    context.addMeasure(context.<Integer>measureBuilder()
      .forMetric(CoreMetrics.DUPLICATED_FILES)
      .onFile(inputFile)
      .withValue(1)
      .build());
    context.addMeasure(context.<Integer>measureBuilder()
      .forMetric(CoreMetrics.DUPLICATED_LINES)
      .onFile(inputFile)
      .withValue(duplicatedLines.size())
      .build());
    context.addMeasure(context.<Integer>measureBuilder()
      .forMetric(CoreMetrics.DUPLICATED_BLOCKS)
      .onFile(inputFile)
      .withValue(duplicatedBlocks)
      .build());

    DuplicationBuilder builder = context.duplicationBuilder(inputFile);
    for (CloneGroup duplication : duplications) {
      builder.originBlock(duplication.getOriginPart().getStartLine(), duplication.getOriginPart().getEndLine());
      for (ClonePart part : duplication.getCloneParts()) {
        if (!part.equals(duplication.getOriginPart())) {
          ((DefaultDuplicationBuilder) builder).isDuplicatedBy(part.getResourceId(), part.getStartLine(), part.getEndLine());
        }
      }
    }
    context.saveDuplications(inputFile, builder.build());
  }

  private static int computeBlockAndLineCount(Iterable<CloneGroup> duplications, Set<Integer> duplicatedLines) {
    int duplicatedBlocks = 0;
    for (CloneGroup clone : duplications) {
      ClonePart origin = clone.getOriginPart();
      for (ClonePart part : clone.getCloneParts()) {
        if (part.getResourceId().equals(origin.getResourceId())) {
          duplicatedBlocks++;
          for (int duplicatedLine = part.getStartLine(); duplicatedLine < part.getStartLine() + part.getLines(); duplicatedLine++) {
            duplicatedLines.add(duplicatedLine);
          }
        }
      }
    }
    return duplicatedBlocks;
  }

}
