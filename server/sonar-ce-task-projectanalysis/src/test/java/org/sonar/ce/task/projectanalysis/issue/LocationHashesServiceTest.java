/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.source.SourceLinesRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.rule.RuleType;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.scanner.protobuf.utils.CloseableIterator;
import org.sonar.server.issue.TaintChecker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class LocationHashesServiceTest {
  private static final String EXAMPLE_LINE_OF_CODE_FORMAT = "int example = line + of + code + %d; ";
  private static final DbCommons.TextRange EXAMPLE_TEXT_RANGE = DbCommons.TextRange.newBuilder()
    .setStartLine(1).setStartOffset(0)
    .setEndLine(3).setEndOffset(EXAMPLE_LINE_OF_CODE_FORMAT.length() - 1)
    .build();
  private static final String LINE_IN_THE_MAIN_FILE = "String string = 'line-in-the-main-file';";
  private static final String ANOTHER_LINE_IN_THE_MAIN_FILE = "String string = 'another-line-in-the-main-file';";
  private static final String LINE_IN_ANOTHER_FILE = "String string = 'line-in-the-another-file';";
  private static final String CHECKSUM = "CHECKSUM";

  private static final RuleKey TAINTED_RULE_KEY = RuleKey.of("javasecurity", "key");
  private static final RuleKey NOT_TAINTED_RULE_KEY = RuleKey.of("java", "key");
  private static final Component FILE_1 = ReportComponent.builder(Component.Type.FILE, 2).build();
  private static final Component FILE_2 = ReportComponent.builder(Component.Type.FILE, 3).build();
  private static final Component ROOT = ReportComponent.builder(Component.Type.PROJECT, 1)
    .addChildren(FILE_1, FILE_2)
    .build();

  private final SourceLinesRepository sourceLinesRepository = mock(SourceLinesRepository.class);
  private final MutableConfiguration configuration = new MutableConfiguration();
  private final TaintChecker taintChecker = new TaintChecker(configuration);
  @RegisterExtension
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5();
  private final LocationHashesService underTest = new LocationHashesService(taintChecker, sourceLinesRepository, treeRootHolder);

  @BeforeEach
  void before() {
    Iterator<String> stringIterator = IntStream.rangeClosed(1, 9)
      .mapToObj(i -> String.format(EXAMPLE_LINE_OF_CODE_FORMAT, i))
      .iterator();
    when(sourceLinesRepository.readLines(FILE_1)).thenReturn(CloseableIterator.from(stringIterator));
    when(sourceLinesRepository.readLines(FILE_2)).thenReturn(newOneLineIterator(LINE_IN_ANOTHER_FILE));
    treeRootHolder.setRoot(ROOT);
    logTester.setLevel(Level.DEBUG);
  }

  @Test
  void beforeCaching_whenIssueUnchangedAndHasChecksum_shouldDoNothing() {
    DefaultIssue notTaintedIssue = createNotTaintedIssue()
      .setLocationsChanged(false)
      .setNew(false)
      .setLocations(DbIssues.Locations.newBuilder()
        .setChecksum(CHECKSUM)
        .setTextRange(EXAMPLE_TEXT_RANGE)
        .build());

    underTest.computeHashesAndUpdateIssues(Collections.emptyList(), List.of(notTaintedIssue), FILE_1);

    DbIssues.Locations locations = notTaintedIssue.getLocations();
    assertThat(locations.getChecksum()).isEqualTo(CHECKSUM);
    verifyNoInteractions(sourceLinesRepository);
  }

  @Test
  void beforeCaching_whenIssueHasNoLocation_shouldDoNothing() {
    DefaultIssue notTaintedIssue = createNotTaintedIssue();

    underTest.computeHashesAndUpdateIssues(List.of(notTaintedIssue), Collections.emptyList(), FILE_1);

    DbIssues.Locations locations = notTaintedIssue.getLocations();
    assertThat(locations).isNull();
    verifyNoInteractions(sourceLinesRepository);
  }

  @Test
  void beforeCaching_whenIssueIsExternal_shouldDoNothing() {
    DefaultIssue notTaintedIssue = createNotTaintedIssue()
      .setIsFromExternalRuleEngine(true)
      .setLocations(DbIssues.Locations.newBuilder()
        .setChecksum(CHECKSUM)
        .setTextRange(EXAMPLE_TEXT_RANGE)
        .build());

    underTest.computeHashesAndUpdateIssues(List.of(notTaintedIssue), Collections.emptyList(), FILE_1);

    DbIssues.Locations locations = notTaintedIssue.getLocations();
    assertThat(locations.getChecksum()).isEqualTo(CHECKSUM);
    verifyNoInteractions(sourceLinesRepository);
  }

  @Test
  void beforeCaching_whenIssueNoLongerExists_shouldDoNothing() {
    DefaultIssue notTaintedIssue = createNotTaintedIssue()
      .setBeingClosed(true)
      .setLocations(DbIssues.Locations.newBuilder()
        .setChecksum(CHECKSUM)
        .setTextRange(EXAMPLE_TEXT_RANGE)
        .build());

    underTest.computeHashesAndUpdateIssues(List.of(notTaintedIssue), Collections.emptyList(), FILE_1);

    DbIssues.Locations locations = notTaintedIssue.getLocations();
    assertThat(locations.getChecksum()).isEqualTo(CHECKSUM);
    verifyNoInteractions(sourceLinesRepository);
  }

  @Test
  void beforeCaching_whenIssueLocationIsOutOfBound_shouldLog() {
    DefaultIssue notTaintedIssue = createNotTaintedIssue()
      .setChecksum(CHECKSUM)
      .setLocations(DbIssues.Locations.newBuilder()
        .setTextRange(DbCommons.TextRange.newBuilder()
          .setStartLine(1).setStartOffset(0)
          .setEndLine(1).setEndOffset(EXAMPLE_LINE_OF_CODE_FORMAT.length() + 1)
          .build())
        .build());

    underTest.computeHashesAndUpdateIssues(List.of(notTaintedIssue), Collections.emptyList(), FILE_1);

    assertThat(logTester.logs(Level.DEBUG)).contains("Try to compute issue location hash from 0 to 38 on line (36 chars): " + String.format(EXAMPLE_LINE_OF_CODE_FORMAT, 1));
  }

  @Test
  void beforeCaching_whenIssueHasNoChecksum_shouldComputeChecksum() {
    DefaultIssue notTaintedIssue = createNotTaintedIssue()
      .setLocationsChanged(false)
      .setNew(false)
      .setLocations(DbIssues.Locations.newBuilder()
        .setTextRange(EXAMPLE_TEXT_RANGE)
        .build());

    underTest.computeHashesAndUpdateIssues(Collections.emptyList(), List.of(notTaintedIssue), FILE_1);

    assertLocationHashIsMadeOf(notTaintedIssue, "intexample=line+of+code+1;intexample=line+of+code+2;intexample=line+of+code+3;");
    verify(sourceLinesRepository).readLines(FILE_1);
  }

  @Test
  void beforeCaching_whenMultipleLinesTaintedIssue_shouldComputeChecksum() {
    DefaultIssue taintedIssue = createTaintedIssue()
      .setLocations(DbIssues.Locations.newBuilder().setTextRange(EXAMPLE_TEXT_RANGE).build());

    underTest.computeHashesAndUpdateIssues(List.of(taintedIssue), Collections.emptyList(), FILE_1);

    assertLocationHashIsMadeOf(taintedIssue, "intexample=line+of+code+1;intexample=line+of+code+2;intexample=line+of+code+3;");
  }

  @Test
  void beforeCaching_whenMultipleTaintedIssuesAndMultipleComponents_shouldComputeAllChecksums() {
    DefaultIssue taintedIssue1 = createTaintedIssue()
      .setLocations(DbIssues.Locations.newBuilder().setTextRange(EXAMPLE_TEXT_RANGE).build());
    DefaultIssue taintedIssue2 = createTaintedIssue()
      .setLocations(DbIssues.Locations.newBuilder().setTextRange(createRange(1, 0, 1, LINE_IN_ANOTHER_FILE.length())).build());

    underTest.computeHashesAndUpdateIssues(List.of(taintedIssue1), Collections.emptyList(), FILE_1);
    underTest.computeHashesAndUpdateIssues(List.of(taintedIssue2), Collections.emptyList(), FILE_2);

    assertLocationHashIsMadeOf(taintedIssue1, "intexample=line+of+code+1;intexample=line+of+code+2;intexample=line+of+code+3;");
    assertLocationHashIsMadeOf(taintedIssue2, "Stringstring='line-in-the-another-file';");
  }

  @Test
  void beforeCaching_whenPartialLineTaintedIssue_shouldComputeChecksum() {
    DefaultIssue taintedIssue = createTaintedIssue()
      .setLocations(DbIssues.Locations.newBuilder().setTextRange(createRange(1, 13, 1, EXAMPLE_LINE_OF_CODE_FORMAT.length() - 1)).build());

    underTest.computeHashesAndUpdateIssues(List.of(taintedIssue), Collections.emptyList(), FILE_1);

    assertLocationHashIsMadeOf(taintedIssue, "line+of+code+1;");
  }

  @Test
  void beforeCaching_whenPartialMultipleLinesTaintedIssue_shouldComputeChecksum() {
    DefaultIssue taintedIssue = createTaintedIssue()
      .setLocations(DbIssues.Locations.newBuilder().setTextRange(createRange(1, 13, 3, 11)).build());

    underTest.computeHashesAndUpdateIssues(List.of(taintedIssue), Collections.emptyList(), FILE_1);

    assertLocationHashIsMadeOf(taintedIssue, "line+of+code+1;intexample=line+of+code+2;intexample");
  }

  @Test
  void beforeCaching_whenNoTextRange_shouldNotComputeChecksum() {
    // primary location and one of the secondary locations have no text range
    DefaultIssue taintedIssue = createTaintedIssue()
      .setLocations(DbIssues.Locations.newBuilder()
        .addFlow(DbIssues.Flow.newBuilder()
          .addLocation(DbIssues.Location.newBuilder()
            .setTextRange(createRange(1, 0, 1, LINE_IN_THE_MAIN_FILE.length()))
            .setComponentId(FILE_1.getUuid())
            .build())
          .addLocation(DbIssues.Location.newBuilder()
            .setComponentId(FILE_2.getUuid())
            .build())
          .build())
        .build());

    when(sourceLinesRepository.readLines(FILE_1)).thenReturn(newOneLineIterator(LINE_IN_THE_MAIN_FILE));

    underTest.computeHashesAndUpdateIssues(List.of(taintedIssue), Collections.emptyList(), FILE_1);

    verify(sourceLinesRepository).readLines(FILE_1);
    verifyNoMoreInteractions(sourceLinesRepository);
    DbIssues.Locations locations = taintedIssue.getLocations();
    assertThat(locations.getFlow(0).getLocation(0).getChecksum()).isEqualTo(DigestUtils.md5Hex("Stringstring='line-in-the-main-file';"));
    assertThat(locations.getFlow(0).getLocation(1).getChecksum()).isEmpty();
  }

  @Test
  void beforeCaching_whenMultipleLocationsInMultipleFiles_shouldComputeAllChecksums() {
    DefaultIssue taintedIssue = createTaintedIssue()
      .setLocations(DbIssues.Locations.newBuilder()
        .setTextRange(createRange(1, 0, 1, LINE_IN_THE_MAIN_FILE.length()))
        .addFlow(DbIssues.Flow.newBuilder()
          .addLocation(DbIssues.Location.newBuilder()
            .setTextRange(createRange(1, 0, 1, LINE_IN_THE_MAIN_FILE.length()))
            .setComponentId(FILE_1.getUuid())
            .build())
          .addLocation(DbIssues.Location.newBuilder()
            .setTextRange(createRange(1, 0, 1, LINE_IN_ANOTHER_FILE.length()))
            .setComponentId(FILE_2.getUuid())
            .build())
          .build())
        .build());

    when(sourceLinesRepository.readLines(FILE_1)).thenReturn(newOneLineIterator(LINE_IN_THE_MAIN_FILE));
    when(sourceLinesRepository.readLines(FILE_2)).thenReturn(newOneLineIterator(LINE_IN_ANOTHER_FILE));

    underTest.computeHashesAndUpdateIssues(List.of(taintedIssue), Collections.emptyList(), FILE_1);

    DbIssues.Locations locations = taintedIssue.getLocations();

    assertThat(locations.getFlow(0).getLocation(0).getChecksum()).isEqualTo(DigestUtils.md5Hex("Stringstring='line-in-the-main-file';"));
    assertThat(locations.getFlow(0).getLocation(1).getChecksum()).isEqualTo(DigestUtils.md5Hex("Stringstring='line-in-the-another-file';"));
  }

  @Test
  void beforeCaching_whenMultipleLocationsInSameFile_shouldComputeAllChecksums() {
    DefaultIssue taintedIssue = createTaintedIssue()
      .setComponentUuid(FILE_1.getUuid())
      .setLocations(DbIssues.Locations.newBuilder()
        .setTextRange(createRange(1, 0, 1, LINE_IN_THE_MAIN_FILE.length()))
        .addFlow(DbIssues.Flow.newBuilder()
          .addLocation(DbIssues.Location.newBuilder()
            .setComponentId(FILE_1.getUuid())
            .setTextRange(createRange(1, 0, 1, LINE_IN_THE_MAIN_FILE.length()))
            .build())
          .addLocation(DbIssues.Location.newBuilder()
            // component id can be empty if location is in the same file
            .setTextRange(createRange(2, 0, 2, ANOTHER_LINE_IN_THE_MAIN_FILE.length()))
            .build())
          .build())
        .build());

    when(sourceLinesRepository.readLines(FILE_1)).thenReturn(manyLinesIterator(LINE_IN_THE_MAIN_FILE, ANOTHER_LINE_IN_THE_MAIN_FILE));

    underTest.computeHashesAndUpdateIssues(List.of(taintedIssue), Collections.emptyList(), FILE_1);

    DbIssues.Locations locations = taintedIssue.getLocations();

    assertThat(locations.getFlow(0).getLocation(0).getChecksum()).isEqualTo(DigestUtils.md5Hex("Stringstring='line-in-the-main-file';"));
    assertThat(locations.getFlow(0).getLocation(1).getChecksum()).isEqualTo(DigestUtils.md5Hex("Stringstring='another-line-in-the-main-file';"));
  }

  @Test
  void beforeCaching_whenNotTaintedIssue_shouldNotComputeChecksumForSecondaryLocations() {
    DefaultIssue notTaintedIssue = createNotTaintedIssue()
      .setLocations(DbIssues.Locations.newBuilder()
        .setTextRange(createRange(1, 0, 1, LINE_IN_THE_MAIN_FILE.length()))
        .addFlow(DbIssues.Flow.newBuilder()
          .addLocation(DbIssues.Location.newBuilder()
            .setTextRange(createRange(1, 0, 1, LINE_IN_THE_MAIN_FILE.length()))
            .setComponentId(FILE_1.getUuid())
            .build())
          .addLocation(DbIssues.Location.newBuilder()
            .setTextRange(createRange(1, 0, 1, LINE_IN_ANOTHER_FILE.length()))
            .setComponentId(FILE_2.getUuid())
            .build())
          .build())
        .build());
    when(sourceLinesRepository.readLines(FILE_1)).thenReturn(newOneLineIterator(LINE_IN_THE_MAIN_FILE));
    when(sourceLinesRepository.readLines(FILE_2)).thenReturn(newOneLineIterator(LINE_IN_ANOTHER_FILE));

    underTest.computeHashesAndUpdateIssues(List.of(notTaintedIssue), Collections.emptyList(), FILE_1);

    DbIssues.Locations locations = notTaintedIssue.getLocations();
    assertLocationHashIsMadeOf(notTaintedIssue, "Stringstring='line-in-the-main-file';");
    assertThat(locations.getFlow(0).getLocation(0).getChecksum()).isEmpty();
    assertThat(locations.getFlow(0).getLocation(1).getChecksum()).isEmpty();
  }

  /** Same whitespace definition as the production MATCH_ALL_WHITESPACES pattern - the compatibility contract. */
  private static final Pattern WHITESPACE = Pattern.compile("\\s");
  /** Must match LocationHashesService.Location.MAX_DIGEST_CHUNK_CHARS. */
  private static final int MAX_DIGEST_CHUNK_CHARS = 8 * 1024;

  @Test
  void streaming_whenAsciiSingleLine_shouldMatchLegacyHash() {
    String line = "int example = line + of + code + 42;";
    DbCommons.TextRange range = createRange(1, 0, 1, line.length());

    assertThat(computeChecksum(range, line)).isEqualTo(legacyHash(rawConcat(range, line)));
  }

  @Test
  void streaming_whenAllWhitespaceCharsInLine_shouldMatchLegacyHash() {
    // space, tab, vertical tab, form feed and carriage return - every \s char that can appear inside a single line
    String line = "a b\tcd\fe\rf";
    DbCommons.TextRange range = createRange(1, 0, 1, line.length());

    String checksum = computeChecksum(range, line);
    assertThat(checksum)
      .isEqualTo(legacyHash(rawConcat(range, line)))
      .isEqualTo(DigestUtils.md5Hex("abcdef"));
  }

  @Test
  void streaming_whenMultiLineRange_shouldMatchLegacyHash() {
    String[] lines = {"first line here", "second line here", "third line here"};
    DbCommons.TextRange range = createRange(1, 0, 3, lines[2].length());

    assertThat(computeChecksum(range, lines)).isEqualTo(legacyHash(rawConcat(range, lines)));
  }

  @Test
  void streaming_whenPartialFirstAndLastLines_shouldMatchLegacyHash() {
    String[] lines = {"first line here", "second line here", "third line here"};
    DbCommons.TextRange range = createRange(1, 6, 3, 5);

    assertThat(computeChecksum(range, lines)).isEqualTo(legacyHash(rawConcat(range, lines)));
  }

  @Test
  void streaming_whenBmpAndSurrogatePairs_shouldMatchLegacyHash() {
    // BMP chars (é, ☃, Ω) plus supplementary code points expressed as surrogate pairs (😀, 𝔘)
    String line = "aé☃b😀cΩd𝔘e";
    DbCommons.TextRange range = createRange(1, 0, 1, line.length());

    assertThat(computeChecksum(range, line)).isEqualTo(legacyHash(rawConcat(range, line)));
  }

  @Test
  void streaming_whenSurrogatePairStraddlesChunkBoundary_shouldMatchLegacyHash() {
    // Fill the chunk so its high surrogate lands exactly on the flush boundary, forcing the pair to split across chunks
    String line = "a".repeat(MAX_DIGEST_CHUNK_CHARS - 1) + "😀" + "b".repeat(10);
    DbCommons.TextRange range = createRange(1, 0, 1, line.length());

    assertThat(computeChecksum(range, line)).isEqualTo(legacyHash(rawConcat(range, line)));
  }

  @Test
  void streaming_whenSurrogatePairStraddlesLaterChunkBoundary_shouldMatchLegacyHash() {
    // One contiguous segment that bulk-append flushes multiple times; the surrogate pair lands on the SECOND
    // flush boundary, exercising trailing-high-surrogate retention on a later flush (not just the first).
    String line = "a".repeat(2 * MAX_DIGEST_CHUNK_CHARS - 1) + "😀" + "b".repeat(10);
    DbCommons.TextRange range = createRange(1, 0, 1, line.length());

    assertThat(computeChecksum(range, line)).isEqualTo(legacyHash(rawConcat(range, line)));
  }

  @Test
  void streaming_whenSurrogatePairStraddlesLineSegments_shouldMatchLegacyHash() {
    // High surrogate ends the first segment, low surrogate starts the next - the pair must still be encoded as one
    String[] lines = {"prefix\uD83D", "\uDE00suffix"};
    DbCommons.TextRange range = createRange(1, 0, 2, lines[1].length());

    assertThat(computeChecksum(range, lines)).isEqualTo(legacyHash(rawConcat(range, lines)));
  }

  @Test
  void streaming_whenWhitespaceStraddlesChunkBoundaryAcrossLines_shouldMatchLegacyHash() {
    // Non-whitespace fills the chunk to its flush point, with spaces/tabs/CR landing exactly on the boundary and
    // spanning the line break. All of it must be stripped before hashing, exactly as the legacy replaceAll("\\s", "").
    String[] lines = {"a".repeat(MAX_DIGEST_CHUNK_CHARS - 1) + " \t\r", "  \tb".repeat(5)};
    DbCommons.TextRange range = createRange(1, 0, 2, lines[1].length());

    assertThat(computeChecksum(range, lines))
      .isEqualTo(legacyHash(rawConcat(range, lines)))
      .isEqualTo(DigestUtils.md5Hex("a".repeat(MAX_DIGEST_CHUNK_CHARS - 1) + "bbbbb"));
  }

  @Test
  void streaming_whenSegmentOneUnderChunkSize_shouldMatchLegacyHash() {
    String line = "a".repeat(MAX_DIGEST_CHUNK_CHARS - 1);
    DbCommons.TextRange range = createRange(1, 0, 1, line.length());

    assertThat(computeChecksum(range, line)).isEqualTo(legacyHash(rawConcat(range, line)));
  }

  @Test
  void streaming_whenSegmentExactlyChunkSize_shouldMatchLegacyHash() {
    String line = "a".repeat(MAX_DIGEST_CHUNK_CHARS);
    DbCommons.TextRange range = createRange(1, 0, 1, line.length());

    assertThat(computeChecksum(range, line)).isEqualTo(legacyHash(rawConcat(range, line)));
  }

  @Test
  void streaming_whenSegmentOneOverChunkSize_shouldMatchLegacyHash() {
    String line = "a".repeat(MAX_DIGEST_CHUNK_CHARS + 1);
    DbCommons.TextRange range = createRange(1, 0, 1, line.length());

    assertThat(computeChecksum(range, line)).isEqualTo(legacyHash(rawConcat(range, line)));
  }

  @Test
  void streaming_whenSegmentSpansMultipleChunks_shouldMatchLegacyHash() {
    String line = "a".repeat(MAX_DIGEST_CHUNK_CHARS * 3 + 17);
    DbCommons.TextRange range = createRange(1, 0, 1, line.length());

    assertThat(computeChecksum(range, line)).isEqualTo(legacyHash(rawConcat(range, line)));
  }

  @Test
  void streaming_whenStartOffsetInvalid_shouldLogAndDigestNothing() {
    String line = "int example = code;";
    DbCommons.TextRange range = createRange(1, -1, 1, line.length());

    assertThat(computeChecksum(range, line)).isEqualTo(legacyHash(""));
    assertThat(logTester.logs(Level.DEBUG)).anyMatch(log -> log.startsWith("Try to compute issue location hash from -1 to"));
  }

  @Test
  void streaming_whenEndOffsetInvalid_shouldLogAndDigestNothing() {
    String line = "int example = code;";
    DbCommons.TextRange range = createRange(1, 0, 1, line.length() + 5);

    assertThat(computeChecksum(range, line)).isEqualTo(legacyHash(""));
    assertThat(logTester.logs(Level.DEBUG)).anyMatch(log -> log.contains("to " + (line.length() + 5) + " on line"));
  }

  @Test
  void streaming_whenBothOffsetsInvalid_shouldLogAndDigestNothing() {
    String line = "int example = code;";
    DbCommons.TextRange range = createRange(1, -3, 1, line.length() + 5);

    assertThat(computeChecksum(range, line)).isEqualTo(legacyHash(""));
    assertThat(logTester.logs(Level.DEBUG)).anyMatch(log -> log.startsWith("Try to compute issue location hash from -3 to " + (line.length() + 5)));
  }

  @Test
  void streaming_whenStartOffsetGreaterThanEndOffset_shouldLogAndDigestNothing() {
    String line = "int example = code;";
    DbCommons.TextRange range = createRange(1, 10, 1, 3);

    assertThat(computeChecksum(range, line)).isEqualTo(legacyHash(""));
    assertThat(logTester.logs(Level.DEBUG)).anyMatch(log -> log.startsWith("Try to compute issue location hash from 10 to 3"));
  }

  @Test
  void streaming_whenMultiMegabyteLine_shouldMatchLegacyHash() {
    // Reproduces the customer allocation shape (a large minified/generated line) at a size safe for the unit suite
    String line = "a".repeat(3 * 1024 * 1024);
    DbCommons.TextRange range = createRange(1, 0, 1, line.length());

    assertThat(computeChecksum(range, line)).isEqualTo(legacyHash(rawConcat(range, line)));
  }

  @Test
  void streaming_whenMultipleLocationsOnSameHugeLine_shouldComputeIndependentChecksums() {
    String line = "abcdefghij".repeat(200 * 1024);
    DbCommons.TextRange primaryRange = createRange(1, 0, 1, line.length());
    DbCommons.TextRange secondaryRange = createRange(1, 5, 1, line.length() - 5);

    DefaultIssue taintedIssue = createTaintedIssue()
      .setComponentUuid(FILE_1.getUuid())
      .setLocations(DbIssues.Locations.newBuilder()
        .setTextRange(primaryRange)
        .addFlow(DbIssues.Flow.newBuilder()
          .addLocation(DbIssues.Location.newBuilder()
            .setComponentId(FILE_1.getUuid())
            .setTextRange(secondaryRange)
            .build())
          .build())
        .build());
    when(sourceLinesRepository.readLines(FILE_1)).thenReturn(newOneLineIterator(line));

    underTest.computeHashesAndUpdateIssues(List.of(taintedIssue), Collections.emptyList(), FILE_1);

    DbIssues.Locations locations = taintedIssue.getLocations();
    assertThat(locations.getChecksum()).isEqualTo(legacyHash(rawConcat(primaryRange, line)));
    assertThat(locations.getFlow(0).getLocation(0).getChecksum()).isEqualTo(legacyHash(rawConcat(secondaryRange, line)));
  }

  /**
   * Drives the service over a single-file, single primary location and returns the computed checksum.
   */
  private String computeChecksum(DbCommons.TextRange range, String... lines) {
    DefaultIssue taintedIssue = createTaintedIssue()
      .setComponentUuid(FILE_1.getUuid())
      .setLocations(DbIssues.Locations.newBuilder().setTextRange(range).build());
    when(sourceLinesRepository.readLines(FILE_1)).thenReturn(manyLinesIterator(lines));

    underTest.computeHashesAndUpdateIssues(List.of(taintedIssue), Collections.emptyList(), FILE_1);

    DbIssues.Locations locations = taintedIssue.getLocations();
    return locations.getChecksum();
  }

  /**
   * Legacy reference algorithm: strip every whitespace char, then MD5 the UTF-8 bytes. This is the compatibility
   * contract the streaming digest must reproduce exactly.
   */
  private static String legacyHash(String locationContent) {
    return DigestUtils.md5Hex(WHITESPACE.matcher(locationContent).replaceAll(""));
  }

  /**
   * Rebuilds the full location content the same way the service derives per-line segment boundaries, before whitespace
   * stripping - the raw input to {@link #legacyHash}.
   */
  private static String rawConcat(DbCommons.TextRange range, String... lines) {
    StringBuilder sb = new StringBuilder();
    for (int lineNumber = range.getStartLine(); lineNumber <= range.getEndLine(); lineNumber++) {
      String line = lines[lineNumber - 1];
      int start;
      int end;
      if (range.getStartLine() == range.getEndLine()) {
        start = range.getStartOffset();
        end = range.getEndOffset();
      } else if (lineNumber == range.getStartLine()) {
        start = range.getStartOffset();
        end = line.length();
      } else if (lineNumber < range.getEndLine()) {
        start = 0;
        end = line.length();
      } else {
        start = 0;
        end = range.getEndOffset();
      }
      sb.append(line, start, end);
    }
    return sb.toString();
  }

  private DbCommons.TextRange createRange(int startLine, int startOffset, int endLine, int endOffset) {
    return DbCommons.TextRange.newBuilder()
      .setStartLine(startLine).setStartOffset(startOffset)
      .setEndLine(endLine).setEndOffset(endOffset)
      .build();
  }

  private DefaultIssue createTaintedIssue() {
    return createIssue(TAINTED_RULE_KEY);
  }

  private DefaultIssue createNotTaintedIssue() {
    return createIssue(NOT_TAINTED_RULE_KEY);
  }

  private DefaultIssue createIssue(RuleKey ruleKey) {
    return new DefaultIssue()
      .setLocationsChanged(true)
      .setRuleKey(ruleKey)
      .setIsFromExternalRuleEngine(false)
      .setType(RuleType.CODE_SMELL);
  }

  private void assertLocationHashIsMadeOf(DefaultIssue issue, String stringToHash) {
    String expectedHash = DigestUtils.md5Hex(stringToHash);
    DbIssues.Locations locations = issue.getLocations();
    assertThat(locations.getChecksum()).isEqualTo(expectedHash);
  }

  private CloseableIterator<String> newOneLineIterator(String lineContent) {
    return CloseableIterator.from(List.of(lineContent).iterator());
  }

  private CloseableIterator<String> manyLinesIterator(String... lines) {
    return CloseableIterator.from(List.of(lines).iterator());
  }

  private static class MutableConfiguration implements Configuration {
    private final Map<String, String> keyValues = new HashMap<>();

    public Configuration put(String key, String value) {
      keyValues.put(key, value.trim());
      return this;
    }

    @Override
    public Optional<String> get(String key) {
      return Optional.ofNullable(keyValues.get(key));
    }

    @Override
    public boolean hasKey(String key) {
      return keyValues.containsKey(key);
    }

    @Override
    public String[] getStringArray(String key) {
      throw new UnsupportedOperationException("getStringArray not implemented");
    }
  }
}
