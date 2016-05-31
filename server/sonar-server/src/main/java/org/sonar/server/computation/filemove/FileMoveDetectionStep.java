/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.filemove;

import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.hash.SourceHashComputer;
import org.sonar.core.hash.SourceLinesHashesComputer;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDtoFunctions;
import org.sonar.db.component.ComponentDtoWithSnapshotId;
import org.sonar.db.component.ComponentTreeQuery;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.computation.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.filemove.FileSimilarity.File;
import org.sonar.server.computation.snapshot.Snapshot;
import org.sonar.server.computation.source.SourceLinesRepository;
import org.sonar.server.computation.step.ComputationStep;

import static com.google.common.base.Splitter.on;
import static com.google.common.collect.FluentIterable.from;
import static java.lang.Math.max;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;

public class FileMoveDetectionStep implements ComputationStep {
  private static final Logger LOG = Loggers.get(FileMoveDetectionStep.class);
  private static final int MIN_REQUIRED_SCORE = 90;
  private static final List<String> FILE_QUALIFIERS = asList(Qualifiers.FILE, Qualifiers.UNIT_TEST_FILE);
  private static final List<String> SORT_FIELDS = singletonList("name");

  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final TreeRootHolder rootHolder;
  private final DbClient dbClient;
  private final SourceLinesRepository sourceLinesRepository;
  private final FileSimilarity fileSimilarity;
  private final MutableMovedFilesRepository movedFilesRepository;

  public FileMoveDetectionStep(AnalysisMetadataHolder analysisMetadataHolder, TreeRootHolder rootHolder, DbClient dbClient,
    SourceLinesRepository sourceLinesRepository, FileSimilarity fileSimilarity, MutableMovedFilesRepository movedFilesRepository) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.rootHolder = rootHolder;
    this.dbClient = dbClient;
    this.sourceLinesRepository = sourceLinesRepository;
    this.fileSimilarity = fileSimilarity;
    this.movedFilesRepository = movedFilesRepository;
  }

  @Override
  public String getDescription() {
    return "Detect file moves";
  }

  @Override
  public void execute() {
    // do nothing if no files in db (first analysis)
    Snapshot baseProjectSnapshot = analysisMetadataHolder.getBaseProjectSnapshot();
    if (baseProjectSnapshot == null) {
      LOG.debug("First analysis. Do nothing.");
      return;
    }

    Map<String, ComponentDtoWithSnapshotId> dbFilesByKey = getDbFilesByKey(baseProjectSnapshot);

    if (dbFilesByKey.isEmpty()) {
      LOG.debug("Previous snapshot has no file. Do nothing.");
      return;
    }

    Map<String, Component> reportFilesByKey = getReportFilesByKey(this.rootHolder.getRoot());

    if (reportFilesByKey.isEmpty()) {
      LOG.debug("No files in report. Do nothing.");
      return;
    }

    Set<String> addedFileKeys = ImmutableSet.copyOf(Sets.difference(reportFilesByKey.keySet(), dbFilesByKey.keySet()));
    Set<String> removedFileKeys = ImmutableSet.copyOf(Sets.difference(dbFilesByKey.keySet(), reportFilesByKey.keySet()));

    // can find matches if at least one of the added or removed files groups is empty => abort
    if (addedFileKeys.isEmpty() || removedFileKeys.isEmpty()) {
      LOG.debug("Either no files added or no files removed. Do nothing.");
      return;
    }

    // retrieve file data from db and report
    Map<String, File> dbFileSourcesByKey = getDbFileSourcesByKey(dbFilesByKey, removedFileKeys);
    Map<String, File> reportFileSourcesByKey = getReportFileSourcesByKey(reportFilesByKey, addedFileKeys);

    // compute score matrix
    ScoreMatrix scoreMatrix = computeScoreMatrix(dbFileSourcesByKey, reportFileSourcesByKey);
    printIfDebug(scoreMatrix);

    // not a single match with score higher than MIN_REQUIRED_SCORE => abort
    if (scoreMatrix.maxScore < MIN_REQUIRED_SCORE) {
      LOG.debug("max score in matrix is less than min required score (%s). Do nothing.", MIN_REQUIRED_SCORE);
      return;
    }

    MatchesByScore matchesByScore = MatchesByScore.create(scoreMatrix);

    ElectedMatches electedMatches = electMatches(dbFileSourcesByKey, reportFileSourcesByKey, matchesByScore);

    for (Match validatedMatch : electedMatches) {
      movedFilesRepository.setOriginalFile(
        reportFilesByKey.get(validatedMatch.getReportKey()),
        toOriginalFile(dbFilesByKey.get(validatedMatch.getDbKey())));
      LOG.info("File move found: " + validatedMatch);
    }
  }

  private Map<String, ComponentDtoWithSnapshotId> getDbFilesByKey(Snapshot baseProjectSnapshot) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return from(dbClient.componentDao().selectAllChildren(
        dbSession,
        ComponentTreeQuery.builder()
          .setBaseSnapshot(new SnapshotDto()
            .setId(baseProjectSnapshot.getId())
            .setRootId(baseProjectSnapshot.getId()))
          .setQualifiers(FILE_QUALIFIERS)
          .setSortFields(SORT_FIELDS)
          .setPageSize(Integer.MAX_VALUE)
          .setPage(1)
          .build()))
            .uniqueIndex(ComponentDtoFunctions.toKey());
    }
  }

  private static Map<String, Component> getReportFilesByKey(Component root) {
    final ImmutableMap.Builder<String, Component> builder = ImmutableMap.builder();
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(CrawlerDepthLimit.FILE, POST_ORDER) {
        @Override
        public void visitFile(Component file) {
          builder.put(file.getKey(), file);
        }
      }).visit(root);
    return builder.build();
  }

  private Map<String, File> getDbFileSourcesByKey(Map<String, ComponentDtoWithSnapshotId> dbFilesByKey, Set<String> removedFileKeys) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ImmutableMap.Builder<String, File> builder = ImmutableMap.builder();
      for (String fileKey : removedFileKeys) {
        ComponentDtoWithSnapshotId componentDto = dbFilesByKey.get(fileKey);
        FileSourceDto fileSourceDto = dbClient.fileSourceDao().selectSourceByFileUuid(dbSession, componentDto.uuid());
        if (fileSourceDto != null) {
          builder.put(fileKey, new File(componentDto.path(), fileSourceDto.getSrcHash(), on('\n').splitToList(fileSourceDto.getLineHashes())));
        }
      }
      return builder.build();
    }
  }

  private Map<String, File> getReportFileSourcesByKey(Map<String, Component> reportFilesByKey, Set<String> addedFileKeys) {
    ImmutableMap.Builder<String, File> builder = ImmutableMap.builder();
    for (String fileKey : addedFileKeys) {
      // FIXME computation of sourceHash and lineHashes might be done multiple times for some files: here, in ComputeFileSourceData, in
      // SourceHashRepository
      Component component = reportFilesByKey.get(fileKey);
      SourceLinesHashesComputer linesHashesComputer = new SourceLinesHashesComputer();
      SourceHashComputer sourceHashComputer = new SourceHashComputer();
      try (CloseableIterator<String> lineIterator = sourceLinesRepository.readLines(component)) {
        while (lineIterator.hasNext()) {
          String line = lineIterator.next();
          linesHashesComputer.addLine(line);
          sourceHashComputer.addLine(line, lineIterator.hasNext());
        }
      }
      builder.put(fileKey, new File(component.getReportAttributes().getPath(), sourceHashComputer.getHash(), linesHashesComputer.getLineHashes()));
    }
    return builder.build();
  }

  private ScoreMatrix computeScoreMatrix(Map<String, File> dbFileSourcesByKey, Map<String, File> reportFileSourcesByKey) {
    int[][] scoreMatrix = new int[dbFileSourcesByKey.size()][reportFileSourcesByKey.size()];
    int maxScore = 0;

    int dbFileIndex = 0;
    for (Map.Entry<String, File> dbFileSourceAndKey : dbFileSourcesByKey.entrySet()) {
      File fileInDb = dbFileSourceAndKey.getValue();
      int reportFileIndex = 0;
      for (Map.Entry<String, File> reportFileSourceAndKey : reportFileSourcesByKey.entrySet()) {
        File unmatchedFile = reportFileSourceAndKey.getValue();
        int score = fileSimilarity.score(fileInDb, unmatchedFile);
        scoreMatrix[dbFileIndex][reportFileIndex] = score;
        if (score > maxScore) {
          maxScore = score;
        }
        reportFileIndex++;
      }
      dbFileIndex++;
    }

    return new ScoreMatrix(dbFileSourcesByKey, reportFileSourcesByKey, scoreMatrix, maxScore);
  }

  private static void printIfDebug(ScoreMatrix scoreMatrix) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("ScoreMatrix:\n" + scoreMatrix.toCsv(';'));
    }
  }

  private static ElectedMatches electMatches(Map<String, File> dbFileSourcesByKey, Map<String, File> reportFileSourcesByKey, MatchesByScore matchesByScore) {
    ElectedMatches electedMatches = new ElectedMatches(matchesByScore, dbFileSourcesByKey, reportFileSourcesByKey);
    Multimap<String, Match> matchesPerFileForScore = ArrayListMultimap.create();
    for (List<Match> matches : matchesByScore) {
      // no match for this score value, ignore
      if (matches == null) {
        continue;
      }

      List<Match> matchesToValidate = electedMatches.filter(matches);
      if (matches.isEmpty()) {
        continue;
      }
      if (matches.size() == 1) {
        Match match = matches.get(0);
        electedMatches.add(match);
      } else {
        matchesPerFileForScore.clear();
        for (Match match : matches) {
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
    return electedMatches;
  }

  private static MovedFilesRepository.OriginalFile toOriginalFile(ComponentDtoWithSnapshotId componentDto) {
    return new MovedFilesRepository.OriginalFile(componentDto.getId(), componentDto.uuid(), componentDto.getKey());
  }

  private static final class ScoreMatrix {
    private final Map<String, File> dbFileSourcesByKey;
    private final Map<String, File> reportFileSourcesByKey;
    private final int[][] scores;
    private final int maxScore;

    public ScoreMatrix(Map<String, File> dbFileSourcesByKey, Map<String, File> reportFileSourcesByKey,
      int[][] scores, int maxScore) {
      this.dbFileSourcesByKey = dbFileSourcesByKey;
      this.reportFileSourcesByKey = reportFileSourcesByKey;
      this.scores = scores;
      this.maxScore = maxScore;
    }

    public void accept(ScoreMatrixVisitor visitor) {
      int dbFileIndex = 0;
      for (Map.Entry<String, File> dbFileSourceAndKey : dbFileSourcesByKey.entrySet()) {
        int reportFileIndex = 0;
        for (Map.Entry<String, File> reportFileSourceAndKey : reportFileSourcesByKey.entrySet()) {
          int score = scores[dbFileIndex][reportFileIndex];
          visitor.visit(dbFileSourceAndKey.getKey(), reportFileSourceAndKey.getKey(), score);
          reportFileIndex++;
        }
        dbFileIndex++;
      }
    }

    public String toCsv(char separator) {
      StringBuilder res = new StringBuilder();
      // first row: empty column, then one column for each report file (its key)
      res.append(separator);
      for (Map.Entry<String, File> reportEntry : reportFileSourcesByKey.entrySet()) {
        res.append(reportEntry.getKey()).append(separator);
      }
      // rows with data: column with db file (its key), then one column for each value
      accept(new ScoreMatrix.ScoreMatrixVisitor() {
        private String previousDbFileKey = null;

        @Override
        public void visit(String dbFileKey, String reportFileKey, int score) {
          if (!Objects.equals(previousDbFileKey, dbFileKey)) {
            res.append('\n').append(dbFileKey).append(separator);
            previousDbFileKey = dbFileKey;
          }
          res.append(score).append(separator);
        }
      });
      return res.toString();
    }

    @FunctionalInterface
    private interface ScoreMatrixVisitor {
      void visit(String dbFileKey, String reportFileKey, int score);
    }
  }

  @Immutable
  private static final class Match {
    private final String dbKey;
    private final String reportKey;

    public Match(String dbKey, String reportKey) {
      this.dbKey = dbKey;
      this.reportKey = reportKey;
    }

    public String getDbKey() {
      return dbKey;
    }

    public String getReportKey() {
      return reportKey;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Match match = (Match) o;
      return dbKey.equals(match.dbKey) && reportKey.equals(match.reportKey);
    }

    @Override
    public int hashCode() {
      return Objects.hash(dbKey, reportKey);
    }

    @Override
    public String toString() {
      return '{' + dbKey + "=>" + reportKey + '}';
    }
  }

  private static class MatchesByScore implements ScoreMatrix.ScoreMatrixVisitor, Iterable<List<Match>> {
    private final ScoreMatrix scoreMatrix;
    private List<Match>[] matches;
    private int totalMatches = 0;

    private MatchesByScore(ScoreMatrix scoreMatrix) {
      this.scoreMatrix = scoreMatrix;
      this.matches = new List[max(MIN_REQUIRED_SCORE, scoreMatrix.maxScore) - MIN_REQUIRED_SCORE];
    }

    public static MatchesByScore create(ScoreMatrix scoreMatrix) {
      MatchesByScore res = new MatchesByScore(scoreMatrix);
      res.populate();
      return res;
    }

    private void populate() {
      scoreMatrix.accept(this);
    }

    @Override
    public void visit(String dbFileKey, String reportFileKey, int score) {
      if (!isAcceptableScore(score)) {
        return;
      }

      int index = score - MIN_REQUIRED_SCORE - 1;
      if (matches[index] == null) {
        matches[index] = new ArrayList<>(1);
      }
      Match match = new Match(dbFileKey, reportFileKey);
      matches[index].add(match);
      totalMatches++;
    }

    public int getSize() {
      return totalMatches;
    }

    @Override
    public Iterator<List<Match>> iterator() {
      return Arrays.asList(matches).iterator();
    }

    private static boolean isAcceptableScore(int score) {
      return score >= MIN_REQUIRED_SCORE;
    }
  }

  private static class ElectedMatches implements Iterable<Match> {
    private final List<Match> matches;
    private final Set<String> matchedFileKeys;
    private final Predicate<Match> notAlreadyMatched = new Predicate<Match>() {
      @Override
      public boolean apply(@Nonnull Match input) {
        return !(matchedFileKeys.contains(input.getDbKey()) || matchedFileKeys.contains(input.getReportKey()));
      }
    };

    public ElectedMatches(MatchesByScore matchesByScore, Map<String, File> dbFileSourcesByKey,
      Map<String, File> reportFileSourcesByKey) {
      this.matches = new ArrayList<>(matchesByScore.getSize());
      this.matchedFileKeys = new HashSet<>(dbFileSourcesByKey.size() + reportFileSourcesByKey.size());
    }

    public void add(Match match) {
      matches.add(match);
      matchedFileKeys.add(match.getDbKey());
      matchedFileKeys.add(match.getReportKey());
    }

    public List<Match> filter(Iterable<Match> matches) {
      return from(matches).filter(notAlreadyMatched).toList();
    }

    @Override
    public Iterator<Match> iterator() {
      return matches.iterator();
    }
  }
}
