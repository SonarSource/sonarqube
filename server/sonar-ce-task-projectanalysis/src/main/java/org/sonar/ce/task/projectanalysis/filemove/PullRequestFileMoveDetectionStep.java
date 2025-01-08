/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.ce.task.projectanalysis.filemove;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.ibatis.session.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.filemove.FileMoveDetectionStep.DbComponent;
import org.sonar.ce.task.projectanalysis.filemove.MovedFilesRepository.OriginalFile;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.FileMoveRowDto;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;

public class PullRequestFileMoveDetectionStep implements ComputationStep {
  private static final Logger LOG = LoggerFactory.getLogger(PullRequestFileMoveDetectionStep.class);

  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final TreeRootHolder rootHolder;
  private final DbClient dbClient;
  private final MutableMovedFilesRepository movedFilesRepository;
  private final MutableAddedFileRepository addedFileRepository;

  public PullRequestFileMoveDetectionStep(AnalysisMetadataHolder analysisMetadataHolder, TreeRootHolder rootHolder, DbClient dbClient,
    MutableMovedFilesRepository movedFilesRepository, MutableAddedFileRepository addedFileRepository) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.rootHolder = rootHolder;
    this.dbClient = dbClient;
    this.movedFilesRepository = movedFilesRepository;
    this.addedFileRepository = addedFileRepository;
  }

  @Override
  public String getDescription() {
    return "Detect file moves in Pull Request scope";
  }

  @Override
  public void execute(ComputationStep.Context context) {
    if (!analysisMetadataHolder.isPullRequest()) {
      LOG.debug("Currently not within Pull Request scope. Do nothing.");
      return;
    }

    Map<String, Component> reportFilesByUuid = getReportFilesByUuid(this.rootHolder.getRoot());
    context.getStatistics().add("reportFiles", reportFilesByUuid.size());

    if (reportFilesByUuid.isEmpty()) {
      LOG.debug("No files in report. No file move detection.");
      return;
    }

    Map<String, DbComponent> targetBranchDbFilesByUuid = getTargetBranchDbFilesByUuid(analysisMetadataHolder);
    context.getStatistics().add("dbFiles", targetBranchDbFilesByUuid.size());

    if (targetBranchDbFilesByUuid.isEmpty()) {
      registerNewlyAddedFiles(reportFilesByUuid);
      context.getStatistics().add("addedFiles", reportFilesByUuid.size());
      LOG.debug("Target branch has no files. No file move detection.");
      return;
    }

    Collection<Component> movedFiles = getMovedFilesByUuid(reportFilesByUuid);
    context.getStatistics().add("movedFiles", movedFiles.size());

    Map<String, Component> newlyAddedFilesByUuid = getNewlyAddedFilesByUuid(reportFilesByUuid, targetBranchDbFilesByUuid);
    context.getStatistics().add("addedFiles", newlyAddedFilesByUuid.size());

    Map<String, DbComponent> dbFilesByPathReference = toDbFilesByPathReferenceMap(targetBranchDbFilesByUuid.values());

    registerMovedFiles(movedFiles, dbFilesByPathReference);
    registerNewlyAddedFiles(newlyAddedFilesByUuid);
  }

  private void registerMovedFiles(Collection<Component> movedFiles, Map<String, DbComponent> dbFilesByPathReference) {
    movedFiles
      .forEach(movedFile -> registerMovedFile(dbFilesByPathReference, movedFile));
  }

  private void registerMovedFile(Map<String, DbComponent> dbFiles, Component movedFile) {
    retrieveDbFile(dbFiles, movedFile)
      .ifPresent(dbFile -> movedFilesRepository.setOriginalPullRequestFile(movedFile, toOriginalFile(dbFile)));
  }

  private void registerNewlyAddedFiles(Map<String, Component> newAddedFilesByUuid) {
    newAddedFilesByUuid
      .values()
      .forEach(addedFileRepository::register);
  }

  private static Map<String, Component> getNewlyAddedFilesByUuid(Map<String, Component> reportFilesByUuid, Map<String, DbComponent> dbFilesByUuid) {
    return reportFilesByUuid
      .values()
      .stream()
      .filter(file -> Objects.isNull(file.getFileAttributes().getOldRelativePath()))
      .filter(file -> !dbFilesByUuid.containsKey(file.getUuid()))
      .collect(toMap(Component::getUuid, identity()));
  }

  private static Collection<Component> getMovedFilesByUuid(Map<String, Component> reportFilesByUuid) {
    return reportFilesByUuid
      .values()
      .stream()
      .filter(file -> Objects.nonNull(file.getFileAttributes().getOldRelativePath()))
      .toList();
  }

  private static Optional<DbComponent> retrieveDbFile(Map<String, DbComponent> dbFilesByPathReference, Component file) {
    return Optional.ofNullable(dbFilesByPathReference.get(file.getFileAttributes().getOldRelativePath()));
  }

  private Map<String, DbComponent> getTargetBranchDbFilesByUuid(AnalysisMetadataHolder analysisMetadataHolder) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return getTargetBranchUuid(dbSession, analysisMetadataHolder.getProject().getUuid(), analysisMetadataHolder.getBranch().getTargetBranchName())
        .map(targetBranchUUid -> getTargetBranchDbFilesByUuid(dbSession, targetBranchUUid))
        .orElse(Map.of());
    }
  }

  private Map<String, DbComponent> getTargetBranchDbFilesByUuid(DbSession dbSession, String targetBranchUuid) {
    Map<String, DbComponent> files = new HashMap<>();
    dbClient.componentDao().scrollAllFilesForFileMove(dbSession, targetBranchUuid, accumulateFilesForFileMove(files));
    return files;
  }

  private static ResultHandler<FileMoveRowDto> accumulateFilesForFileMove(Map<String, DbComponent> accumulator) {
    return resultContext -> {
      DbComponent component = rowToDbComponent(resultContext.getResultObject());
      accumulator.put(component.uuid(), component);
    };
  }

  private static DbComponent rowToDbComponent(FileMoveRowDto row) {
    return new DbComponent(row.getKey(), row.getUuid(), row.getPath(), row.getLineCount());
  }

  private Optional<String> getTargetBranchUuid(DbSession dbSession, String projectUuid, String targetBranchName) {
    return dbClient.branchDao().selectByBranchKey(dbSession, projectUuid, targetBranchName)
      .map(BranchDto::getUuid);
  }

  private static Map<String, DbComponent> toDbFilesByPathReferenceMap(Collection<DbComponent> dbFiles) {
    return dbFiles
      .stream()
      .collect(toMap(DbComponent::path, identity()));
  }

  private static Map<String, Component> getReportFilesByUuid(Component root) {
    final ImmutableMap.Builder<String, Component> builder = ImmutableMap.builder();

    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(CrawlerDepthLimit.FILE, POST_ORDER) {
        @Override
        public void visitFile(Component file) {
          builder.put(file.getUuid(), file);
        }
      }).visit(root);

    return builder.build();
  }

  private static OriginalFile toOriginalFile(DbComponent dbComponent) {
    return new OriginalFile(dbComponent.uuid(), dbComponent.key());
  }
}
