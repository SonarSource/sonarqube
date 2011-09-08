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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.Logs;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.BlockChunker;
import org.sonar.duplications.detector.original.OriginalCloneDetectionAlgorithm;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.ClonePart;
import org.sonar.duplications.java.JavaStatementBuilder;
import org.sonar.duplications.java.JavaTokenProducer;
import org.sonar.duplications.statement.Statement;
import org.sonar.duplications.statement.StatementChunker;
import org.sonar.duplications.token.TokenChunker;
import org.sonar.plugins.cpd.index.DbDuplicationsIndex;
import org.sonar.plugins.cpd.index.SonarDuplicationsIndex;

public class SonarEngine extends CpdEngine {

  private static final int BLOCK_SIZE = 10;

  private final ResourcePersister resourcePersister;
  private final DatabaseSession dbSession;

  /**
   * For dry run, where is no access to database.
   */
  public SonarEngine() {
    this(null, null);
  }

  public SonarEngine(ResourcePersister resourcePersister, DatabaseSession dbSession) {
    this.resourcePersister = resourcePersister;
    this.dbSession = dbSession;
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
        && resourcePersister != null && dbSession != null
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
      Logs.INFO.info("Cross-project analysis enabled");
      index = new SonarDuplicationsIndex(new DbDuplicationsIndex(dbSession, resourcePersister, project));
    } else {
      Logs.INFO.info("Cross-project analysis disabled");
      index = new SonarDuplicationsIndex();
    }

    TokenChunker tokenChunker = JavaTokenProducer.build();
    StatementChunker statementChunker = JavaStatementBuilder.build();
    BlockChunker blockChunker = new BlockChunker(BLOCK_SIZE);

    for (InputFile inputFile : inputFiles) {
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
    for (InputFile inputFile : inputFiles) {
      Resource resource = getResource(inputFile);
      String resourceKey = getFullKey(project, resource);

      Collection<Block> fileBlocks = index.getByResource(resource, resourceKey);
      List<CloneGroup> clones = OriginalCloneDetectionAlgorithm.detect(index, fileBlocks);
      if (!clones.isEmpty()) {
        // Save
        DuplicationsData data = new DuplicationsData(resource, context);
        for (CloneGroup clone : clones) {
          poplulateData(data, clone);
        }
        data.save();
      }
    }
  }

  private Resource getResource(InputFile inputFile) {
    return JavaFile.fromRelativePath(inputFile.getRelativePath(), false);
  }

  private void poplulateData(DuplicationsData data, CloneGroup clone) {
    ClonePart origin = clone.getOriginPart();
    int originLines = origin.getLineEnd() - origin.getLineStart() + 1;

    data.incrementDuplicatedBlock();
    for (ClonePart part : clone.getCloneParts()) {
      if (part.equals(origin)) {
        continue;
      }
      data.cumulate(part.getResourceId(), part.getLineStart(), origin.getLineStart(), originLines);

      if (part.getResourceId().equals(origin.getResourceId())) {
        data.incrementDuplicatedBlock();
        data.cumulate(origin.getResourceId(), origin.getLineStart(), part.getLineStart(), originLines);
      }
    }
  }

}
