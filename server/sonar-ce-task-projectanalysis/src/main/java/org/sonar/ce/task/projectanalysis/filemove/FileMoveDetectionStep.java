/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
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
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.FileMoveRowDto;
import org.sonar.db.source.LineHashesWithKeyDto;

import static com.google.common.collect.FluentIterable.from;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;

public class FileMoveDetectionStep implements ComputationStep {
  static final int MIN_REQUIRED_SCORE = 85;
  private static final Logger LOG = Loggers.get(FileMoveDetectionStep.class);
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

  public FileMoveDetectionStep(AnalysisMetadataHolder analysisMetadataHolder, TreeRootHolder rootHolder, DbClient dbClient,
    FileSimilarity fileSimilarity, MutableMovedFilesRepository movedFilesRepository, SourceLinesHashRepository sourceLinesHash,
    ScoreMatrixDumper scoreMatrixDumper, MutableAddedFileRepository addedFileRepository) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.rootHolder = rootHolder;
    this.dbClient = dbClient;
    this.fileSimilarity = fileSimilarity;
    this.movedFilesRepository = movedFilesRepository;
    this.sourceLinesHash = sourceLinesHash;
    this.scoreMatrixDumper = scoreMatrixDumper;
    this.addedFileRepository = addedFileRepository;
  }

  @Override
  public String getDescription() {
    return "Detect file moves";
  }

  @Override
  public void execute(ComputationStep.Context context) {
    // do nothing if no files in db (first analysis)
    if (analysisMetadataHolder.isFirstAnalysis()) {
      LOG.debug("First analysis. Do nothing.");
      return;
    }
    Profiler p = Profiler.createIfTrace(LOG);

    p.start();
    Map<String, Component> reportFilesByKey = getReportFilesByKey(this.rootHolder.getRoot());
    context.getStatistics().add("reportFiles", reportFilesByKey.size());
    if (reportFilesByKey.isEmpty()) {
      LOG.debug("No files in report. No file move detection.");
      return;
    }

    Map<String, DbComponent> dbFilesByKey = getDbFilesByKey();
    context.getStatistics().add("dbFiles", dbFilesByKey.size());

    Set<String> addedFileKeys = difference(reportFilesByKey.keySet(), dbFilesByKey.keySet());
    context.getStatistics().add("addedFiles", addedFileKeys.size());

    if (dbFilesByKey.isEmpty()) {
      registerAddedFiles(addedFileKeys, reportFilesByKey, null);
      LOG.debug("Previous snapshot has no file. No file move detection.");
      return;
    }

    Set<String> removedFileKeys = difference(dbFilesByKey.keySet(), reportFilesByKey.keySet());

    // can find matches if at least one of the added or removed files groups is empty => abort
    if (addedFileKeys.isEmpty() || removedFileKeys.isEmpty()) {
      registerAddedFiles(addedFileKeys, reportFilesByKey, null);
      LOG.debug("Either no files added or no files removed. Do nothing.");
      return;
    }

    // retrieve file data from report
    Map<String, File> reportFileSourcesByKey = getReportFileSourcesByKey(reportFilesByKey, addedFileKeys);
    p.stopTrace("loaded");

    // compute score matrix
    p.start();
    ScoreMatrix scoreMatrix = computeScoreMatrix(dbFilesByKey, removedFileKeys, reportFileSourcesByKey);
    p.stopTrace("Score matrix computed");
    scoreMatrixDumper.dumpAsCsv(scoreMatrix);

    // not a single match with score higher than MIN_REQUIRED_SCORE => abort
    if (scoreMatrix.getMaxScore() < MIN_REQUIRED_SCORE) {
      context.getStatistics().add("movedFiles", 0);
      registerAddedFiles(addedFileKeys, reportFilesByKey, null);
      LOG.debug("max score in matrix is less than min required score ({}). Do nothing.", MIN_REQUIRED_SCORE);
      return;
    }

    p.start();
    MatchesByScore matchesByScore = MatchesByScore.create(scoreMatrix);

    ElectedMatches electedMatches = electMatches(removedFileKeys, reportFileSourcesByKey, matchesByScore);
    p.stopTrace("Matches elected");

    context.getStatistics().add("movedFiles", electedMatches.size());
    registerMatches(dbFilesByKey, reportFilesByKey, electedMatches);
    registerAddedFiles(addedFileKeys, reportFilesByKey, electedMatches);
  }

  public Set<String> difference(Set<String> set1, Set<String> set2) {
    if (set1.isEmpty() || set2.isEmpty()) {
      return set1;
    }
    return Sets.difference(set1, set2).immutableCopy();
  }

  private void registerMatches(Map<String, DbComponent> dbFilesByKey, Map<String, Component> reportFilesByKey, ElectedMatches electedMatches) {
    LOG.debug("{} files moves found", electedMatches.size());
    for (Match validatedMatch : electedMatches) {
      movedFilesRepository.setOriginalFile(
        reportFilesByKey.get(validatedMatch.getReportKey()),
        toOriginalFile(dbFilesByKey.get(validatedMatch.getDbKey())));
      LOG.trace("File move found: {}", validatedMatch);
    }
  }

  private void registerAddedFiles(Set<String> addedFileKeys, Map<String, Component> reportFilesByKey, @Nullable ElectedMatches electedMatches) {
    if (electedMatches == null || electedMatches.isEmpty()) {
      addedFileKeys.stream()
        .map(reportFilesByKey::get)
        .forEach(addedFileRepository::register);
    } else {
      Set<String> reallyAddedFileKeys = new HashSet<>(addedFileKeys);
      for (Match electedMatch : electedMatches) {
        reallyAddedFileKeys.remove(electedMatch.getReportKey());
      }
      reallyAddedFileKeys.stream()
        .map(reportFilesByKey::get)
        .forEach(addedFileRepository::register);
    }
  }

  private Map<String, DbComponent> getDbFilesByKey() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ImmutableList.Builder<DbComponent> builder = ImmutableList.builder();
      dbClient.componentDao().scrollAllFilesForFileMove(dbSession, rootHolder.getRoot().getUuid(),
        resultContext -> {
          FileMoveRowDto row = resultContext.getResultObject();
          builder.add(new DbComponent(row.getId(), row.getKey(), row.getUuid(), row.getPath(), row.getLineCount()));
        });
      return builder.build().stream()
        .collect(MoreCollectors.uniqueIndex(DbComponent::getKey));
    }
  }

  private static Map<String, Component> getReportFilesByKey(Component root) {
    final ImmutableMap.Builder<String, Component> builder = ImmutableMap.builder();
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(CrawlerDepthLimit.FILE, POST_ORDER) {
        @Override
        public void visitFile(Component file) {
          builder.put(file.getDbKey(), file);
        }
      }).visit(root);
    return builder.build();
  }

  private Map<String, File> getReportFileSourcesByKey(Map<String, Component> reportFilesByKey, Set<String> addedFileKeys) {
    ImmutableMap.Builder<String, File> builder = ImmutableMap.builder();
    for (String fileKey : addedFileKeys) {
      Component component = reportFilesByKey.get(fileKey);
      File file = new LazyFileImpl(
        component.getReportAttributes().getPath(),
        () -> getReportFileLineHashes(component),
        component.getFileAttributes().getLines());
      builder.put(fileKey, file);
    }
    return builder.build();
  }

  private List<String> getReportFileLineHashes(Component component) {
    return sourceLinesHash.getLineHashesMatchingDBVersion(component);
  }

  private ScoreMatrix computeScoreMatrix(Map<String, DbComponent> dtosByKey, Set<String> removedFileKeys, Map<String, File> newFileSourcesByKey) {
    ScoreMatrix.ScoreFile[] newFiles = newFileSourcesByKey.entrySet().stream()
      .map(e -> new ScoreMatrix.ScoreFile(e.getKey(), e.getValue().getLineCount()))
      .toArray(ScoreMatrix.ScoreFile[]::new);
    ScoreMatrix.ScoreFile[] removedFiles = removedFileKeys.stream()
      .map(key -> {
        DbComponent dbComponent = dtosByKey.get(key);
        return new ScoreMatrix.ScoreFile(dbComponent.getKey(), dbComponent.getLineCount());
      })
      .toArray(ScoreMatrix.ScoreFile[]::new);
    // sort by highest line count first
    Arrays.sort(newFiles, SCORE_FILE_COMPARATOR);
    Arrays.sort(removedFiles, SCORE_FILE_COMPARATOR);
    int[][] scoreMatrix = new int[removedFiles.length][newFiles.length];
    int lastNewFileIndex = newFiles.length - 1;

    Map<String, Integer> removedFilesIndexes = new HashMap<>(removedFileKeys.size());
    for (int removeFileIndex = 0; removeFileIndex < removedFiles.length; removeFileIndex++) {
      ScoreMatrix.ScoreFile removedFile = removedFiles[removeFileIndex];
      int lowerBound = (int) Math.floor(removedFile.getLineCount() * LOWER_BOUND_RATIO);
      int upperBound = (int) Math.ceil(removedFile.getLineCount() * UPPER_BOUND_RATIO);
      // no need to compute score if all files are out of bound, so no need to load line hashes from DB
      if (newFiles[0].getLineCount() <= lowerBound || newFiles[lastNewFileIndex].getLineCount() >= upperBound) {
        continue;
      }
      removedFilesIndexes.put(removedFile.getFileKey(), removeFileIndex);
    }

    LineHashesWithKeyDtoResultHandler rowHandler = new LineHashesWithKeyDtoResultHandler(removedFilesIndexes, removedFiles,
      newFiles, newFileSourcesByKey, scoreMatrix);
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.fileSourceDao().scrollLineHashes(dbSession, removedFilesIndexes.keySet(), rowHandler);
    }

    return new ScoreMatrix(removedFiles, newFiles, scoreMatrix, rowHandler.getMaxScore());
  }

  private final class LineHashesWithKeyDtoResultHandler implements ResultHandler<LineHashesWithKeyDto> {
    private final Map<String, Integer> removedFilesIndexes;
    private final ScoreMatrix.ScoreFile[] removedFiles;
    private final ScoreMatrix.ScoreFile[] newFiles;
    private final Map<String, File> newFileSourcesByKey;
    private final int[][] scoreMatrix;
    private int maxScore;

    private LineHashesWithKeyDtoResultHandler(Map<String, Integer> removedFilesIndexes, ScoreMatrix.ScoreFile[] removedFiles,
      ScoreMatrix.ScoreFile[] newFiles, Map<String, File> newFileSourcesByKey,
      int[][] scoreMatrix) {
      this.removedFilesIndexes = removedFilesIndexes;
      this.removedFiles = removedFiles;
      this.newFiles = newFiles;
      this.newFileSourcesByKey = newFileSourcesByKey;
      this.scoreMatrix = scoreMatrix;
    }

    @Override
    public void handleResult(ResultContext<? extends LineHashesWithKeyDto> resultContext) {
      LineHashesWithKeyDto lineHashesDto = resultContext.getResultObject();
      if (lineHashesDto.getPath() == null) {
        return;
      }
      int removeFileIndex = removedFilesIndexes.get(lineHashesDto.getKey());
      ScoreMatrix.ScoreFile removedFile = removedFiles[removeFileIndex];
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

        File fileInDb = new FileImpl(lineHashesDto.getPath(), lineHashesDto.getLineHashes());
        File unmatchedFile = newFileSourcesByKey.get(newFile.getFileKey());
        int score = fileSimilarity.score(fileInDb, unmatchedFile);
        scoreMatrix[removeFileIndex][newFileIndex] = score;
        if (score > maxScore) {
          maxScore = score;
        }
      }
    }

    int getMaxScore() {
      return maxScore;
    }
  }

  private static ElectedMatches electMatches(Set<String> dbFileKeys, Map<String, File> reportFileSourcesByKey, MatchesByScore matchesByScore) {
    ElectedMatches electedMatches = new ElectedMatches(matchesByScore, dbFileKeys, reportFileSourcesByKey);
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
        matchesPerFileForScore.put(match.getDbKey(), match);
        matchesPerFileForScore.put(match.getReportKey(), match);
      }
      // validate non ambiguous matches (ie. the match is the only match of either the db file and the report file)
      for (Match match : matchesToValidate) {
        int dbFileMatchesCount = matchesPerFileForScore.get(match.getDbKey()).size();
        int reportFileMatchesCount = matchesPerFileForScore.get(match.getReportKey()).size();
        if (dbFileMatchesCount == 1 && reportFileMatchesCount == 1) {
          electedMatches.add(match);
        }
      }
    }
  }

  private static MovedFilesRepository.OriginalFile toOriginalFile(DbComponent dbComponent) {
    return new MovedFilesRepository.OriginalFile(dbComponent.getId(), dbComponent.getUuid(), dbComponent.getKey());
  }

  @Immutable
  private static final class DbComponent {
    private final long id;
    private final String key;
    private final String uuid;
    private final String path;
    private final int lineCount;

    private DbComponent(long id, String key, String uuid, String path, int lineCount) {
      this.id = id;
      this.key = key;
      this.uuid = uuid;
      this.path = path;
      this.lineCount = lineCount;
    }

    public long getId() {
      return id;
    }

    public String getKey() {
      return key;
    }

    public String getUuid() {
      return uuid;
    }

    public String getPath() {
      return path;
    }

    public int getLineCount() {
      return lineCount;
    }
  }

  private static class ElectedMatches implements Iterable<Match> {
    private final List<Match> matches;
    private final Set<String> matchedFileKeys;

    public ElectedMatches(MatchesByScore matchesByScore, Set<String> dbFileKeys, Map<String, File> reportFileSourcesByKey) {
      this.matches = new ArrayList<>(matchesByScore.getSize());
      this.matchedFileKeys = new HashSet<>(dbFileKeys.size() + reportFileSourcesByKey.size());
    }

    public void add(Match match) {
      matches.add(match);
      matchedFileKeys.add(match.getDbKey());
      matchedFileKeys.add(match.getReportKey());
    }

    public List<Match> filter(Iterable<Match> matches) {
      return from(matches).filter(this::notAlreadyMatched).toList();
    }

    private boolean notAlreadyMatched(Match input) {
      return !(matchedFileKeys.contains(input.getDbKey()) || matchedFileKeys.contains(input.getReportKey()));
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
