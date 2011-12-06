/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.cpd;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.*;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.index.ResourcePersister;
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
import org.sonar.persistence.dao.DuplicationDao;
import org.sonar.plugins.cpd.index.DbDuplicationsIndex;
import org.sonar.plugins.cpd.index.SonarDuplicationsIndex;

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

  private final ResourcePersister resourcePersister;
  private final DuplicationDao dao;

  /**
   * For dry run, where is no access to database.
   */
  public SonarEngine() {
    this(null, null);
  }

  public SonarEngine(ResourcePersister resourcePersister, DuplicationDao dao) {
    this.resourcePersister = resourcePersister;
    this.dao = dao;
  }

  @Override
  public boolean isLanguageSupported(Language language) {
    return Java.INSTANCE.equals(language);
  }

  /**
   * @return true, if was enabled by user and database is available
   */
  private boolean isCrossProject(Project project) {
    return project.getConfiguration().getBoolean(CoreProperties.CPD_CROSS_RPOJECT, CoreProperties.CPD_CROSS_RPOJECT_DEFAULT_VALUE)
      && resourcePersister != null && dao != null
      && StringUtils.isBlank(project.getConfiguration().getString(CoreProperties.PROJECT_BRANCH_PROPERTY));
  }

  private static String getFullKey(Project project, Resource resource) {
    return new StringBuilder(ResourceModel.KEY_SIZE)
      .append(project.getKey())
      .append(':')
      .append(resource.getKey())
      .toString();
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    List<InputFile> inputFiles = project.getFileSystem().mainFiles(project.getLanguageKey());
    if (inputFiles.isEmpty()) {
      return;
    }

    // Create index
    final SonarDuplicationsIndex index;
    if (isCrossProject(project)) {
      LOG.info("Cross-project analysis enabled");
      index = new SonarDuplicationsIndex(new DbDuplicationsIndex(resourcePersister, project, dao));
    } else {
      LOG.info("Cross-project analysis disabled");
      index = new SonarDuplicationsIndex();
    }

    TokenChunker tokenChunker = JavaTokenProducer.build();
    StatementChunker statementChunker = JavaStatementBuilder.build();
    BlockChunker blockChunker = new BlockChunker(BLOCK_SIZE);

    for (InputFile inputFile : inputFiles) {
      LOG.debug("Populating index from {}", inputFile.getFile());
      Resource resource = getResource(inputFile);
      String resourceKey = getFullKey(project, resource);

      List<Statement> statements;

      Reader reader = null;
      try {
        reader = new InputStreamReader(new FileInputStream(inputFile.getFile()), project.getFileSystem().getSourceCharset());
        statements = statementChunker.chunk(tokenChunker.chunk(reader));
      } catch (FileNotFoundException e) {
        throw new SonarException(e);
      } finally {
        IOUtils.closeQuietly(reader);
      }

      List<Block> blocks = blockChunker.chunk(resourceKey, statements);
      index.insert(resource, blocks);
    }

    // Detect
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    try {
      for (InputFile inputFile : inputFiles) {
        LOG.debug("Detection of duplications for {}", inputFile.getFile());
        Resource resource = getResource(inputFile);
        String resourceKey = getFullKey(project, resource);

        Collection<Block> fileBlocks = index.getByResource(resource, resourceKey);

        List<CloneGroup> clones;
        try {
          clones = executorService.submit(new Task(index, fileBlocks)).get(TIMEOUT, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
          clones = null;
          LOG.warn("Timeout during detection of duplications for " + inputFile.getFile(), e);
        } catch (InterruptedException e) {
          throw new SonarException(e);
        } catch (ExecutionException e) {
          throw new SonarException(e);
        }

        save(context, resource, clones);
      }
    } finally {
      executorService.shutdown();
    }
  }

  private static class Task implements Callable<List<CloneGroup>> {
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

  private Resource getResource(InputFile inputFile) {
    return JavaFile.fromRelativePath(inputFile.getRelativePath(), false);
  }

  static void save(SensorContext context, Resource resource, List<CloneGroup> clones) {
    if (clones == null || clones.isEmpty()) {
      return;
    }
    // Calculate number of lines and blocks
    Set<Integer> duplicatedLines = new HashSet<Integer>();
    double duplicatedBlocks = 0;
    for (CloneGroup clone : clones) {
      ClonePart origin = clone.getOriginPart();
      for (ClonePart part : clone.getCloneParts()) {
        if (part.getResourceId().equals(origin.getResourceId())) {
          duplicatedBlocks++;
          for (int duplicatedLine = part.getLineStart(); duplicatedLine < part.getLineStart() + part.getLines(); duplicatedLine++) {
            duplicatedLines.add(duplicatedLine);
          }
        }
      }
    }
    // Build XML
    StringBuilder xml = new StringBuilder();
    xml.append("<duplications>");
    for (CloneGroup clone : clones) {
      xml.append("<g>");
      for (ClonePart part : clone.getCloneParts()) {
        xml.append("<b s=\"").append(part.getLineStart())
            .append("\" l=\"").append(part.getLines())
            .append("\" r=\"").append(part.getResourceId())
            .append("\"/>");
      }
      xml.append("</g>");
    }
    xml.append("</duplications>");
    // Save
    context.saveMeasure(resource, CoreMetrics.DUPLICATED_FILES, 1d);
    context.saveMeasure(resource, CoreMetrics.DUPLICATED_LINES, (double) duplicatedLines.size());
    context.saveMeasure(resource, CoreMetrics.DUPLICATED_BLOCKS, duplicatedBlocks);
    context.saveMeasure(resource, new Measure(CoreMetrics.DUPLICATIONS_DATA, xml.toString()));
  }

}
