/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.scm;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.Component.Status;
import org.sonar.ce.task.projectanalysis.source.SourceHashRepository;
import org.sonar.ce.task.projectanalysis.source.SourceLinesDiff;

import static java.util.Objects.requireNonNull;

public class ScmInfoRepositoryImpl implements ScmInfoRepository {

  private static final Logger LOGGER = Loggers.get(ScmInfoRepositoryImpl.class);

  private final BatchReportReader scannerReportReader;
  private final Map<Component, Optional<ScmInfo>> scmInfoCache = new HashMap<>();
  private final ScmInfoDbLoader scmInfoDbLoader;
  private final AnalysisMetadataHolder analysisMetadata;
  private final SourceLinesDiff sourceLinesDiff;
  private final SourceHashRepository sourceHashRepository;

  public ScmInfoRepositoryImpl(BatchReportReader scannerReportReader, AnalysisMetadataHolder analysisMetadata, ScmInfoDbLoader scmInfoDbLoader,
                               SourceLinesDiff sourceLinesDiff, SourceHashRepository sourceHashRepository) {
    this.scannerReportReader = scannerReportReader;
    this.analysisMetadata = analysisMetadata;
    this.scmInfoDbLoader = scmInfoDbLoader;
    this.sourceLinesDiff = sourceLinesDiff;
    this.sourceHashRepository = sourceHashRepository;
  }

  @Override
  public Optional<ScmInfo> getScmInfo(Component component) {
    requireNonNull(component, "Component cannot be null");

    if (component.getType() != Component.Type.FILE) {
      return Optional.empty();
    }

    return scmInfoCache.computeIfAbsent(component, this::getScmInfoForComponent);
  }

  private Optional<ScmInfo> getScmInfoForComponent(Component component) {
    ScannerReport.Changesets changesets = scannerReportReader.readChangesets(component.getReportAttributes().getRef());

    if (changesets == null) {
      LOGGER.trace("No SCM info for file '{}'", component.getDbKey());
      // SCM not available. It might have been available before - copy information for unchanged lines but don't keep author and revision.
      return generateAndMergeDb(component, false);
    }

    // will be empty if the flag "copy from previous" is set, or if the file is empty.
    if (changesets.getChangesetCount() == 0) {
      return generateAndMergeDb(component, changesets.getCopyFromPrevious());
    }
    return getScmInfoFromReport(component, changesets);
  }

  private static Optional<ScmInfo> getScmInfoFromReport(Component file, ScannerReport.Changesets changesets) {
    LOGGER.trace("Reading SCM info from report for file '{}'", file.getDbKey());
    return Optional.of(new ReportScmInfo(changesets));
  }

  private Optional<ScmInfo> generateScmInfoForAllFile(Component file) {
    if (file.getFileAttributes().getLines() == 0) {
      return Optional.empty();
    }
    Set<Integer> newOrChangedLines = IntStream.rangeClosed(1, file.getFileAttributes().getLines()).boxed().collect(Collectors.toSet());
    return Optional.of(GeneratedScmInfo.create(analysisMetadata.getAnalysisDate(), newOrChangedLines));
  }

  private static ScmInfo removeAuthorAndRevision(ScmInfo info) {
    Map<Integer, Changeset> cleanedScmInfo = info.getAllChangesets().entrySet().stream()
      .collect(Collectors.toMap(Map.Entry::getKey, e -> removeAuthorAndRevision(e.getValue())));
    return new ScmInfoImpl(cleanedScmInfo);
  }

  private static Changeset removeAuthorAndRevision(Changeset changeset) {
    return Changeset.newChangesetBuilder().setDate(changeset.getDate()).build();
  }

  /**
   * Get SCM information in the DB, if it exists, and use it for lines that didn't change. It optionally removes author and revision
   * information (only keeping change dates).
   * If the information is not present in the DB or some lines don't match existing lines in the DB,
   * we generate change dates based on the analysis date.
   */
  private Optional<ScmInfo> generateAndMergeDb(Component file, boolean keepAuthorAndRevision) {
    Optional<DbScmInfo> dbInfoOpt = scmInfoDbLoader.getScmInfo(file);
    if (!dbInfoOpt.isPresent()) {
      return generateScmInfoForAllFile(file);
    }

    ScmInfo scmInfo = keepAuthorAndRevision ? dbInfoOpt.get() : removeAuthorAndRevision(dbInfoOpt.get());
    boolean fileUnchanged = file.getStatus() == Status.SAME && sourceHashRepository.getRawSourceHash(file).equals(dbInfoOpt.get().fileHash());

    if (fileUnchanged) {
      return Optional.of(scmInfo);
    }

    // generate date for new/changed lines
    int[] matchingLines = sourceLinesDiff.computeMatchingLines(file);

    return Optional.of(GeneratedScmInfo.create(analysisMetadata.getAnalysisDate(), matchingLines, scmInfo));
  }

}
