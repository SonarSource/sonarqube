/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.filemove.FileSimilarity.File;
import org.sonar.ce.task.projectanalysis.filemove.FileSimilarity.FileImpl;
import org.sonar.ce.task.projectanalysis.filemove.FileSimilarity.LazyFileImpl;
import org.sonar.ce.task.projectanalysis.source.SourceLinesHashRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.util.logs.Profiler;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.FileMoveRowDto;
import org.sonar.db.source.LineHashesWithUuidDto;

import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;

public class FileMoveDetectionStep implements ComputationStep {
  static final int MIN_REQUIRED_SCORE = 85;
  private static final Logger LOG = LoggerFactory.getLogger(FileMoveDetectionStep.class);
  private static final Comparator<ScoreMatrix.ScoreFile> SCORE_FILE_COMPARATOR = (o1, o2) -> -1 * Integer.compare(o1.getLineCount(), o2.getLineCount());
  private static final double LOWER_BOUND_RATIO = 0.84;
  private static final double UPPER_BOUND_RATIO = 1.18;

  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final TreeRootHolder rootHolder;
  private final DbClient dbClient;
  private final FileSimilarity fileSimilarity;
  private final MutableMovedFilesRepository movedFilesRepository;
  private final SourceLinesHashRepository sourceLinesHash;
  private final ScoreMatrixDumper scoreMatrixDumper;
  private final MutableAddedFileRepository addedFileRepository;
  private final HeapSizeChecker heapSizeChecker;

  public FileMoveDetectionStep(AnalysisMetadataHolder analysisMetadataHolder, TreeRootHolder rootHolder, DbClient dbClient,
    FileSimilarity fileSimilarity, MutableMovedFilesRepository movedFilesRepository, SourceLinesHashRepository sourceLinesHash,
    ScoreMatrixDumper scoreMatrixDumper, MutableAddedFileRepository addedFileRepository, HeapSizeChecker heapSizeChecker) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.rootHolder = rootHolder;
    this.dbClient = dbClient;
    this.fileSimilarity = fileSimilarity;
    this.movedFilesRepository = movedFilesRepository;
    this.sourceLinesHash = sourceLinesHash;
    this.scoreMatrixDumper = scoreMatrixDumper;
    this.addedFileRepository = addedFileRepository;
    this.heapSizeChecker = heapSizeChecker;
  }

  @Override
  public String getDescription() {
    return "Detect file moves";
  }

  @Override
  public void execute(ComputationStep.Context context) {
    if (analysisMetadataHolder.isPullRequest()) {
      LOG.debug("Currently within Pull Request scope. Do nothing.");
      return;
    }

    // do nothing if no files in db (first analysis)
    if (analysisMetadataHolder.isFirstAnalysis()) {
      LOG.debug("First analysis. Do nothing.");
      return;
    }
    Profiler p = Profiler.createIfTrace(LOG);

    p.start();
    Map<String, Component> reportFilesByUuid = getReportFilesByUuid(this.rootHolder.getRoot());
    context.getStatistics().add("reportFiles", reportFilesByUuid.size());
    if (reportFilesByUuid.isEmpty()) {
      LOG.debug("No files in report. No file move detection.");
      return;
    }

    Map<String, DbComponent> dbFilesByUuid = getDbFilesByUuid();
    context.getStatistics().add("dbFiles", dbFilesByUuid.size());

    Set<String> addedFileUuids = difference(reportFilesByUuid.keySet(), dbFilesByUuid.keySet());
    context.getStatistics().add("addedFiles", addedFileUuids.size());

    if (dbFilesByUuid.isEmpty()) {
      registerAddedFiles(addedFileUuids, reportFilesByUuid, null);
      LOG.debug("Previous snapshot has no file. No file move detection.");
      return;
    }

    Set<String> removedFileUuids = difference(dbFilesByUuid.keySet(), reportFilesByUuid.keySet());

    // can't find matches if at least one of the added or removed files groups is empty => abort
    if (addedFileUuids.isEmpty() || removedFileUuids.isEmpty()) {
      registerAddedFiles(addedFileUuids, reportFilesByUuid, null);
      LOG.debug("Either no files added or no files removed. Do nothing.");
      return;
    }

    // retrieve file data from report
    Map<String, File> addedFileHashesByUuid = getReportFileHashesByUuid(reportFilesByUuid, addedFileUuids);
    p.stopTrace("loaded");

    // compute score matrix
    p.start();
    ScoreMatrix scoreMatrix = computeScoreMatrix(dbFilesByUuid, removedFileUuids, addedFileHashesByUuid);
    p.stopTrace("Score matrix computed");
    scoreMatrixDumper.dumpAsCsv(scoreMatrix);

    // not a single match with score higher than MIN_REQUIRED_SCORE => abort
    if (scoreMatrix.getMaxScore() < MIN_REQUIRED_SCORE) {
      context.getStatistics().add("movedFiles", 0);
      registerAddedFiles(addedFileUuids, reportFilesByUuid, null);
      LOG.debug("max score in matrix is less than min required score ({}). Do nothing.", MIN_REQUIRED_SCORE);
      return;
    }

    p.start();
    MatchesByScore matchesByScore = MatchesByScore.create(scoreMatrix);

    ElectedMatches electedMatches = electMatches(removedFileUuids, addedFileHashesByUuid, matchesByScore);
    p.stopTrace("Matches elected");

    context.getStatistics().add("movedFiles", electedMatches.size());
    registerMatches(dbFilesByUuid, reportFilesByUuid, electedMatches);
    registerAddedFiles(addedFileUuids, reportFilesByUuid, electedMatches);
  }

  public Set<String> difference(Set<String> set1, Set<String> set2) {
    if (set1.isEmpty() || set2.isEmpty()) {
      return set1;
    }
    return Sets.difference(set1, set2).immutableCopy();
  }

  private void registerMatches(Map<String, DbComponent> dbFilesByUuid, Map<String, Component> reportFilesByUuid, ElectedMatches electedMatches) {
    LOG.debug("{} files moves found", electedMatches.size());
    for (Match validatedMatch : electedMatches) {
      movedFilesRepository.setOriginalFile(
        reportFilesByUuid.get(validatedMatch.reportUuid()),
        toOriginalFile(dbFilesByUuid.get(validatedMatch.dbUuid())));
      LOG.trace("File move found: {}", validatedMatch);
    }
  }

  private void registerAddedFiles(Set<String> addedFileUuids, Map<String, Component> reportFilesByUuid, @Nullable ElectedMatches electedMatches) {
    if (electedMatches == null || electedMatches.isEmpty()) {
      addedFileUuids.stream()
        .map(reportFilesByUuid::get)
        .forEach(addedFileRepository::register);
    } else {
      Set<String> reallyAddedFileUuids = new HashSet<>(addedFileUuids);
      for (Match electedMatch : electedMatches) {
        reallyAddedFileUuids.remove(electedMatch.reportUuid());
      }
      reallyAddedFileUuids.stream()
        .map(reportFilesByUuid::get)
        .forEach(addedFileRepository::register);
    }
  }

  private Map<String, DbComponent> getDbFilesByUuid() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ImmutableList.Builder<DbComponent> builder = ImmutableList.builder();
      dbClient.componentDao().scrollAllFilesForFileMove(dbSession, rootHolder.getRoot().getUuid(),
        resultContext -> {
          FileMoveRowDto row = resultContext.getResultObject();
          builder.add(new DbComponent(row.getKey(), row.getUuid(), row.getPath(), row.getLineCount()));
        });
      return builder.build().stream()
        .collect(Collectors.toMap(DbComponent::uuid, Function.identity()));
    }
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

  private Map<String, File> getReportFileHashesByUuid(Map<String, Component> reportFilesByUuid, Set<String> addedFileUuids) {
    return addedFileUuids.stream().collect(Collectors.toMap(fileUuid -> fileUuid, fileUuid -> {
      Component component = reportFilesByUuid.get(fileUuid);
      return new LazyFileImpl(() -> getReportFileLineHashes(component), component.getFileAttributes().getLines());
    }));
  }

  private List<String> getReportFileLineHashes(Component component) {
    // this is not ideal because if the file moved, this component won't exist in DB with the same UUID.
    // Assuming that the file also had significant code before the move, it will be fine.
    return sourceLinesHash.getLineHashesMatchingDBVersion(component);
  }

  private ScoreMatrix computeScoreMatrix(Map<String, DbComponent> dtosByUuid, Set<String> removedFileUuids, Map<String, File> addedFileHashesByUuid) {
    ScoreMatrix.ScoreFile[] addedFiles = addedFileHashesByUuid.entrySet().stream()
      .map(e -> new ScoreMatrix.ScoreFile(e.getKey(), e.getValue().getLineCount()))
      .toArray(ScoreMatrix.ScoreFile[]::new);
    ScoreMatrix.ScoreFile[] removedFiles = removedFileUuids.stream()
      .map(key -> {
        DbComponent dbComponent = dtosByUuid.get(key);
        return new ScoreMatrix.ScoreFile(dbComponent.uuid(), dbComponent.lineCount());
      })
      .toArray(ScoreMatrix.ScoreFile[]::new);

    int totalAddedFiles = addedFiles.length;
    int totalRemovedFiles = removedFiles.length;

    heapSizeChecker.checkHeapLimits(totalAddedFiles, totalRemovedFiles);

    // sort by highest line count first
    Arrays.sort(addedFiles, SCORE_FILE_COMPARATOR);
    Arrays.sort(removedFiles, SCORE_FILE_COMPARATOR);
    int[][] scoreMatrix = new int[totalRemovedFiles][totalAddedFiles];
    int highestAddedFileLineCount = addedFiles[0].getLineCount();
    int lowestAddedFileLineCount = addedFiles[totalAddedFiles - 1].getLineCount();

    Map<String, Integer> removedFilesIndexesByUuid = HashMap.newHashMap(removedFileUuids.size());
    for (int removeFileIndex = 0; removeFileIndex < totalRemovedFiles; removeFileIndex++) {
      ScoreMatrix.ScoreFile removedFile = removedFiles[removeFileIndex];
      int lowerBound = (int) Math.floor(removedFile.getLineCount() * LOWER_BOUND_RATIO);
      int upperBound = (int) Math.ceil(removedFile.getLineCount() * UPPER_BOUND_RATIO);
      // no need to compute score if all files are out of bound, so no need to load line hashes from DB
      if (highestAddedFileLineCount <= lowerBound || lowestAddedFileLineCount >= upperBound) {
        continue;
      }
      removedFilesIndexesByUuid.put(removedFile.getFileUuid(), removeFileIndex);
    }

    LineHashesWithKeyDtoResultHandler rowHandler = new LineHashesWithKeyDtoResultHandler(removedFilesIndexesByUuid, removedFiles,
      addedFiles, addedFileHashesByUuid, scoreMatrix);
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.fileSourceDao().scrollLineHashes(dbSession, removedFilesIndexesByUuid.keySet(), rowHandler);
    }

    return new ScoreMatrix(removedFiles, addedFiles, scoreMatrix, rowHandler.getMaxScore());
  }

  private final class LineHashesWithKeyDtoResultHandler implements ResultHandler<LineHashesWithUuidDto> {
    private final Map<String, Integer> removedFileIndexesByUuid;
    private final ScoreMatrix.ScoreFile[] removedFiles;
    private final ScoreMatrix.ScoreFile[] newFiles;
    private final Map<String, File> newFilesByUuid;
    private final int[][] scoreMatrix;
    private int maxScore;

    private LineHashesWithKeyDtoResultHandler(Map<String, Integer> removedFileIndexesByUuid, ScoreMatrix.ScoreFile[] removedFiles,
      ScoreMatrix.ScoreFile[] newFiles, Map<String, File> newFilesByUuid,
      int[][] scoreMatrix) {
      this.removedFileIndexesByUuid = removedFileIndexesByUuid;
      this.removedFiles = removedFiles;
      this.newFiles = newFiles;
      this.newFilesByUuid = newFilesByUuid;
      this.scoreMatrix = scoreMatrix;
    }

    @Override
    public void handleResult(ResultContext<? extends LineHashesWithUuidDto> resultContext) {
      LineHashesWithUuidDto lineHashesDto = resultContext.getResultObject();
      if (lineHashesDto.getPath() == null) {
        return;
      }
      int removedFileIndex = removedFileIndexesByUuid.get(lineHashesDto.getUuid());
      ScoreMatrix.ScoreFile removedFile = removedFiles[removedFileIndex];
      int lowerBound = (int) Math.floor(removedFile.getLineCount() * LOWER_BOUND_RATIO);
      int upperBound = (int) Math.ceil(removedFile.getLineCount() * UPPER_BOUND_RATIO);

      for (int newFileIndex = 0; newFileIndex < newFiles.length; newFileIndex++) {
        ScoreMatrix.ScoreFile newFile = newFiles[newFileIndex];
        if (newFile.getLineCount() >= upperBound) {
          continue;
        }
        if (newFile.getLineCount() <= lowerBound) {
          break;
        }

        File fileHashesInDb = new FileImpl(lineHashesDto.getLineHashes());
        File unmatchedFile = newFilesByUuid.get(newFile.getFileUuid());
        int score = fileSimilarity.score(fileHashesInDb, unmatchedFile);
        scoreMatrix[removedFileIndex][newFileIndex] = score;
        if (score > maxScore) {
          maxScore = score;
        }
      }
    }

    int getMaxScore() {
      return maxScore;
    }
  }

  private static ElectedMatches electMatches(Set<String> dbFileUuids, Map<String, File> reportFileSourcesByUuid, MatchesByScore matchesByScore) {
    ElectedMatches electedMatches = new ElectedMatches(matchesByScore, dbFileUuids, reportFileSourcesByUuid);
    Multimap<String, Match> matchesPerFileForScore = ArrayListMultimap.create();
    matchesByScore.forEach(matches -> electMatches(matches, electedMatches, matchesPerFileForScore));
    return electedMatches;
  }

  private static void electMatches(@Nullable List<Match> matches, ElectedMatches electedMatches, Multimap<String, Match> matchesPerFileForScore) {
    // no match for this score value, ignore
    if (matches == null) {
      return;
    }

    List<Match> matchesToValidate = electedMatches.filter(matches);
    if (matchesToValidate.isEmpty()) {
      return;
    }
    if (matchesToValidate.size() == 1) {
      Match match = matchesToValidate.get(0);
      electedMatches.add(match);
    } else {
      matchesPerFileForScore.clear();
      for (Match match : matchesToValidate) {
        matchesPerFileForScore.put(match.dbUuid(), match);
        matchesPerFileForScore.put(match.reportUuid(), match);
      }
      // validate non-ambiguous matches (i.e. the match is the only match of either the db file and the report file)
      for (Match match : matchesToValidate) {
        int dbFileMatchesCount = matchesPerFileForScore.get(match.dbUuid()).size();
        int reportFileMatchesCount = matchesPerFileForScore.get(match.reportUuid()).size();
        if (dbFileMatchesCount == 1 && reportFileMatchesCount == 1) {
          electedMatches.add(match);
        }
      }
    }
  }

  private static MovedFilesRepository.OriginalFile toOriginalFile(DbComponent dbComponent) {
    return new MovedFilesRepository.OriginalFile(dbComponent.uuid(), dbComponent.key());
  }

  public record DbComponent(String key, String uuid, String path, int lineCount) {
  }

  private static class ElectedMatches implements Iterable<Match> {
    private final List<Match> matches;
    private final Set<String> matchedFileUuids;

    public ElectedMatches(MatchesByScore matchesByScore, Set<String> dbFileUuids, Map<String, File> reportFileHashesByUuid) {
      this.matches = new ArrayList<>(matchesByScore.getSize());
      this.matchedFileUuids =  HashSet.newHashSet(dbFileUuids.size() + reportFileHashesByUuid.size());
    }

    public void add(Match match) {
      matches.add(match);
      matchedFileUuids.add(match.dbUuid());
      matchedFileUuids.add(match.reportUuid());
    }

    public List<Match> filter(Collection<Match> matches) {
      return matches.stream().filter(this::notAlreadyMatched).toList();
    }

    private boolean notAlreadyMatched(Match input) {
      return !(matchedFileUuids.contains(input.dbUuid()) || matchedFileUuids.contains(input.reportUuid()));
    }

    @Override
    public Iterator<Match> iterator() {
      return matches.iterator();
    }

    public int size() {
      return matches.size();
    }

    public boolean isEmpty() {
      return matches.isEmpty();
    }
  }
}
