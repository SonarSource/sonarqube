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
package org.sonar.server.computation.task.projectanalysis.filemove;

import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.hash.SourceLinesHashesComputer;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTreeQuery;
import org.sonar.db.component.ComponentTreeQuery.Strategy;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.task.projectanalysis.filemove.FileSimilarity.File;
import org.sonar.server.computation.task.projectanalysis.source.SourceLinesRepository;
import org.sonar.server.computation.task.step.ComputationStep;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Splitter.on;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Arrays.asList;
import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;

public class FileMoveDetectionStep implements ComputationStep {
  protected static final int MIN_REQUIRED_SCORE = 85;
  private static final Logger LOG = Loggers.get(FileMoveDetectionStep.class);
  private static final List<String> FILE_QUALIFIERS = asList(Qualifiers.FILE, Qualifiers.UNIT_TEST_FILE);
  private static final Splitter LINES_HASHES_SPLITTER = on('\n');

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
    if (analysisMetadataHolder.isFirstAnalysis()) {
      LOG.debug("First analysis. Do nothing.");
      return;
    }

    Map<String, DbComponent> dbFilesByKey = getDbFilesByKey();
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

    // retrieve file data from report
    Map<String, File> reportFileSourcesByKey = getReportFileSourcesByKey(reportFilesByKey, addedFileKeys);

    // compute score matrix
    ScoreMatrix scoreMatrix = computeScoreMatrix(dbFilesByKey, removedFileKeys, reportFileSourcesByKey);
    printIfDebug(scoreMatrix);

    // not a single match with score higher than MIN_REQUIRED_SCORE => abort
    if (scoreMatrix.getMaxScore() < MIN_REQUIRED_SCORE) {
      LOG.debug("max score in matrix is less than min required score (%s). Do nothing.", MIN_REQUIRED_SCORE);
      return;
    }

    MatchesByScore matchesByScore = MatchesByScore.create(scoreMatrix);

    ElectedMatches electedMatches = electMatches(removedFileKeys, reportFileSourcesByKey, matchesByScore);

    registerMatches(dbFilesByKey, reportFilesByKey, electedMatches);
  }

  private void registerMatches(Map<String, DbComponent> dbFilesByKey, Map<String, Component> reportFilesByKey, ElectedMatches electedMatches) {
    for (Match validatedMatch : electedMatches) {
      movedFilesRepository.setOriginalFile(
        reportFilesByKey.get(validatedMatch.getReportKey()),
        toOriginalFile(dbFilesByKey.get(validatedMatch.getDbKey())));
      LOG.debug("File move found: {}", validatedMatch);
    }
  }

  private Map<String, DbComponent> getDbFilesByKey() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      // FIXME no need to use such a complex query, joining on SNAPSHOTS and retrieving all column of table PROJECTS, replace with dedicated
      // mapper method
      List<ComponentDto> componentDtos = dbClient.componentDao().selectDescendants(
        dbSession,
        ComponentTreeQuery.builder()
          .setBaseUuid(rootHolder.getRoot().getUuid())
          .setQualifiers(FILE_QUALIFIERS)
          .setStrategy(Strategy.LEAVES)
          .build());
      return from(componentDtos)
        .transform(componentDto -> new DbComponent(componentDto.getId(), componentDto.getDbKey(), componentDto.uuid(), componentDto.path()))
        .uniqueIndex(DbComponent::getKey);
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

  private Map<String, File> getReportFileSourcesByKey(Map<String, Component> reportFilesByKey, Set<String> addedFileKeys) {
    ImmutableMap.Builder<String, File> builder = ImmutableMap.builder();
    for (String fileKey : addedFileKeys) {
      // FIXME computation of sourceHash and lineHashes might be done multiple times for some files: here, in ComputeFileSourceData, in
      // SourceHashRepository
      Component component = reportFilesByKey.get(fileKey);
      SourceLinesHashesComputer linesHashesComputer = new SourceLinesHashesComputer();
      try (CloseableIterator<String> lineIterator = sourceLinesRepository.readLines(component)) {
        while (lineIterator.hasNext()) {
          String line = lineIterator.next();
          linesHashesComputer.addLine(line);
        }
      }
      builder.put(fileKey, new File(component.getReportAttributes().getPath(), linesHashesComputer.getLineHashes()));
    }
    return builder.build();
  }

  private ScoreMatrix computeScoreMatrix(Map<String, DbComponent> dtosByKey, Set<String> dbFileKeys, Map<String, File> reportFileSourcesByKey) {
    int[][] scoreMatrix = new int[dbFileKeys.size()][reportFileSourcesByKey.size()];
    int maxScore = 0;

    try (DbSession dbSession = dbClient.openSession(false)) {
      int dbFileIndex = 0;
      for (String removedFileKey : dbFileKeys) {
        File fileInDb = getFile(dbSession, dtosByKey.get(removedFileKey));
        if (fileInDb == null) {
          continue;
        }

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
    }

    return new ScoreMatrix(dbFileKeys, reportFileSourcesByKey, scoreMatrix, maxScore);
  }

  @CheckForNull
  private File getFile(DbSession dbSession, DbComponent dbComponent) {
    if (dbComponent.getPath() == null) {
      return null;
    }
    FileSourceDto fileSourceDto = dbClient.fileSourceDao().selectSourceByFileUuid(dbSession, dbComponent.getUuid());
    if (fileSourceDto == null) {
      return null;
    }
    String lineHashes = firstNonNull(fileSourceDto.getLineHashes(), "");
    return new File(dbComponent.getPath(), LINES_HASHES_SPLITTER.splitToList(lineHashes));
  }

  private static void printIfDebug(ScoreMatrix scoreMatrix) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("ScoreMatrix:\n" + scoreMatrix.toCsv(';'));
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

    private DbComponent(long id, String key, String uuid, String path) {
      this.id = id;
      this.key = key;
      this.uuid = uuid;
      this.path = path;
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
  }
}
