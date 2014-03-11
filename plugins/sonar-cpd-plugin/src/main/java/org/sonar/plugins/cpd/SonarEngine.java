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
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.PersistenceMode;
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
import java.util.concurrent.*;

public class SonarEngine extends CpdEngine {

  private static final Logger LOG = LoggerFactory.getLogger(SonarEngine.class);

  private static final int BLOCK_SIZE = 10;

  /**
   * Limit of time to analyse one file (in seconds).
   */
  private static final int TIMEOUT = 5 * 60;

  private final IndexFactory indexFactory;
  private final FileSystem fs;
  private final Settings settings;

  public SonarEngine(IndexFactory indexFactory, FileSystem fs, Settings settings) {
    this.indexFactory = indexFactory;
    this.fs = fs;
    this.settings = settings;
  }

  @Override
  public boolean isLanguageSupported(String language) {
    return "java".equals(language);
  }

  @Override
  public void analyse(Project project, String languageKey, SensorContext context) {
    String[] cpdExclusions = settings.getStringArray(CoreProperties.CPD_EXCLUSIONS);
    logExclusions(cpdExclusions, LOG);
    List<InputFile> sourceFiles = Lists.newArrayList(fs.inputFiles(FilePredicates.and(
      FilePredicates.hasType(InputFile.Type.MAIN),
      FilePredicates.hasLanguage(languageKey),
      FilePredicates.doesNotMatchPathPatterns(cpdExclusions)
    )));
    if (sourceFiles.isEmpty()) {
      return;
    }
    SonarDuplicationsIndex index = createIndex(project, sourceFiles);
    detect(index, context, sourceFiles);
  }

  private SonarDuplicationsIndex createIndex(Project project, Iterable<InputFile> sourceFiles) {
    final SonarDuplicationsIndex index = indexFactory.create(project);

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
        throw new SonarException("Cannot find file " + inputFile.file(), e);
      } finally {
        IOUtils.closeQuietly(reader);
      }

      List<Block> blocks = blockChunker.chunk(resourceEffectiveKey, statements);
      index.insert(inputFile, blocks);
    }

    return index;
  }

  private void detect(SonarDuplicationsIndex index, SensorContext context, List<InputFile> sourceFiles) {
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
          clones = null;
          LOG.warn("Timeout during detection of duplications for " + inputFile, e);
        } catch (InterruptedException e) {
          throw new SonarException("Fail during detection of duplication for " + inputFile, e);
        } catch (ExecutionException e) {
          throw new SonarException("Fail during detection of duplication for " + inputFile, e);
        }

        save(context, inputFile, clones);
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

  static void save(SensorContext context, InputFile inputFile, @Nullable Iterable<CloneGroup> duplications) {
    if (duplications == null || Iterables.isEmpty(duplications)) {
      return;
    }
    // Calculate number of lines and blocks
    Set<Integer> duplicatedLines = new HashSet<Integer>();
    double duplicatedBlocks = 0;
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
    // Save
    context.saveMeasure(inputFile, CoreMetrics.DUPLICATED_FILES, 1.0);
    context.saveMeasure(inputFile, CoreMetrics.DUPLICATED_LINES, (double) duplicatedLines.size());
    context.saveMeasure(inputFile, CoreMetrics.DUPLICATED_BLOCKS, duplicatedBlocks);

    Measure data = new Measure(CoreMetrics.DUPLICATIONS_DATA, toXml(duplications))
      .setPersistenceMode(PersistenceMode.DATABASE);
    context.saveMeasure(inputFile, data);
  }

  private static String toXml(Iterable<CloneGroup> duplications) {
    StringBuilder xml = new StringBuilder();
    xml.append("<duplications>");
    for (CloneGroup duplication : duplications) {
      xml.append("<g>");
      for (ClonePart part : duplication.getCloneParts()) {
        xml.append("<b s=\"").append(part.getStartLine())
          .append("\" l=\"").append(part.getLines())
          .append("\" r=\"").append(StringEscapeUtils.escapeXml(part.getResourceId()))
          .append("\"/>");
      }
      xml.append("</g>");
    }
    xml.append("</duplications>");
    return xml.toString();
  }

}
