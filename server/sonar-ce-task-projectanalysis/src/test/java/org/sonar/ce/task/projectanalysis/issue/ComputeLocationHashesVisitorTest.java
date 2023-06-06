/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.source.SourceLinesRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.server.issue.TaintChecker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ComputeLocationHashesVisitorTest {
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
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public LogTester logTester = new LogTester();
  private final ComputeLocationHashesVisitor underTest = new ComputeLocationHashesVisitor(taintChecker, sourceLinesRepository, treeRootHolder);

  @Before
  public void before() {
    Iterator<String> stringIterator = IntStream.rangeClosed(1, 9)
      .mapToObj(i -> String.format(EXAMPLE_LINE_OF_CODE_FORMAT, i))
      .iterator();
    when(sourceLinesRepository.readLines(FILE_1)).thenReturn(CloseableIterator.from(stringIterator));
    when(sourceLinesRepository.readLines(FILE_2)).thenReturn(newOneLineIterator(LINE_IN_ANOTHER_FILE));
    treeRootHolder.setRoot(ROOT);
    logTester.setLevel(Level.DEBUG);
  }

  @Test
  public void beforeCaching_whenIssueUnchangedAndHasChecksum_shouldDoNothing() {
    DefaultIssue notTaintedIssue = createNotTaintedIssue()
      .setLocationsChanged(false)
      .setNew(false)
      .setLocations(DbIssues.Locations.newBuilder()
        .setChecksum(CHECKSUM)
        .setTextRange(EXAMPLE_TEXT_RANGE)
        .build());

    executeBeforeCaching(FILE_1, notTaintedIssue);

    DbIssues.Locations locations = notTaintedIssue.getLocations();
    assertThat(locations.getChecksum()).isEqualTo(CHECKSUM);
    verifyNoInteractions(sourceLinesRepository);
  }

  @Test
  public void beforeCaching_whenIssueHasNoLocation_shouldDoNothing() {
    DefaultIssue notTaintedIssue = createNotTaintedIssue();

    executeBeforeCaching(FILE_1, notTaintedIssue);

    DbIssues.Locations locations = notTaintedIssue.getLocations();
    assertThat(locations).isNull();
    verifyNoInteractions(sourceLinesRepository);
  }

  @Test
  public void beforeCaching_whenIssueIsExternal_shouldDoNothing() {
    DefaultIssue notTaintedIssue = createNotTaintedIssue()
      .setIsFromExternalRuleEngine(true)
      .setLocations(DbIssues.Locations.newBuilder()
        .setChecksum(CHECKSUM)
        .setTextRange(EXAMPLE_TEXT_RANGE)
        .build());;

    executeBeforeCaching(FILE_1, notTaintedIssue);

    DbIssues.Locations locations = notTaintedIssue.getLocations();
    assertThat(locations.getChecksum()).isEqualTo(CHECKSUM);
    verifyNoInteractions(sourceLinesRepository);
  }

  @Test
  public void beforeCaching_whenIssueNoLongerExists_shouldDoNothing() {
    DefaultIssue notTaintedIssue = createNotTaintedIssue()
      .setBeingClosed(true)
      .setLocations(DbIssues.Locations.newBuilder()
        .setChecksum(CHECKSUM)
        .setTextRange(EXAMPLE_TEXT_RANGE)
        .build());;

    executeBeforeCaching(FILE_1, notTaintedIssue);

    DbIssues.Locations locations = notTaintedIssue.getLocations();
    assertThat(locations.getChecksum()).isEqualTo(CHECKSUM);
    verifyNoInteractions(sourceLinesRepository);
  }

  @Test
  public void beforeCaching_whenIssueLocationIsOutOfBound_shouldLog() {
    DefaultIssue notTaintedIssue = createNotTaintedIssue()
      .setChecksum(CHECKSUM)
      .setLocations(DbIssues.Locations.newBuilder()
        .setTextRange(DbCommons.TextRange.newBuilder()
          .setStartLine(1).setStartOffset(0)
          .setEndLine(1).setEndOffset(EXAMPLE_LINE_OF_CODE_FORMAT.length() + 1)
          .build())
        .build());

    executeBeforeCaching(FILE_1, notTaintedIssue);

    assertThat(logTester.logs(Level.DEBUG)).contains("Try to compute issue location hash from 0 to 38 on line (36 chars): " + String.format(EXAMPLE_LINE_OF_CODE_FORMAT, 1));
  }

  @Test
  public void beforeCaching_whenIssueHasNoChecksum_shouldComputeChecksum() {
    DefaultIssue notTaintedIssue = createNotTaintedIssue()
      .setLocationsChanged(false)
      .setNew(false)
      .setLocations(DbIssues.Locations.newBuilder()
        .setTextRange(EXAMPLE_TEXT_RANGE)
        .build());

    executeBeforeCaching(FILE_1, notTaintedIssue);

    assertLocationHashIsMadeOf(notTaintedIssue, "intexample=line+of+code+1;intexample=line+of+code+2;intexample=line+of+code+3;");
    verify(sourceLinesRepository).readLines(FILE_1);
  }

  @Test
  public void beforeCaching_whenMultipleLinesTaintedIssue_shouldComputeChecksum() {
    DefaultIssue taintedIssue = createTaintedIssue()
      .setLocations(DbIssues.Locations.newBuilder().setTextRange(EXAMPLE_TEXT_RANGE).build());

    executeBeforeCaching(FILE_1, taintedIssue);

    assertLocationHashIsMadeOf(taintedIssue, "intexample=line+of+code+1;intexample=line+of+code+2;intexample=line+of+code+3;");
  }

  @Test
  public void beforeCaching_whenMultipleTaintedIssuesAndMultipleComponents_shouldComputeAllChecksums() {
    DefaultIssue taintedIssue1 = createTaintedIssue()
      .setLocations(DbIssues.Locations.newBuilder().setTextRange(EXAMPLE_TEXT_RANGE).build());
    DefaultIssue taintedIssue2 = createTaintedIssue()
      .setLocations(DbIssues.Locations.newBuilder().setTextRange(createRange(1, 0, 1, LINE_IN_ANOTHER_FILE.length())).build());

    executeBeforeCaching(FILE_1, taintedIssue1);
    executeBeforeCaching(FILE_2, taintedIssue2);

    assertLocationHashIsMadeOf(taintedIssue1, "intexample=line+of+code+1;intexample=line+of+code+2;intexample=line+of+code+3;");
    assertLocationHashIsMadeOf(taintedIssue2, "Stringstring='line-in-the-another-file';");
  }

  @Test
  public void beforeCaching_whenPartialLineTaintedIssue_shouldComputeChecksum() {
    DefaultIssue taintedIssue = createTaintedIssue()
      .setLocations(DbIssues.Locations.newBuilder().setTextRange(createRange(1, 13, 1, EXAMPLE_LINE_OF_CODE_FORMAT.length() - 1)).build());

    executeBeforeCaching(FILE_1, taintedIssue);

    assertLocationHashIsMadeOf(taintedIssue, "line+of+code+1;");
  }

  @Test
  public void beforeCaching_whenPartialMultipleLinesTaintedIssue_shouldComputeChecksum() {
    DefaultIssue taintedIssue = createTaintedIssue()
      .setLocations(DbIssues.Locations.newBuilder().setTextRange(createRange(1, 13, 3, 11)).build());

    executeBeforeCaching(FILE_1, taintedIssue);

    assertLocationHashIsMadeOf(taintedIssue, "line+of+code+1;intexample=line+of+code+2;intexample");
  }

  @Test
  public void beforeCaching_whenNoTextRange_shouldNotComputeChecksum() {
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

    executeBeforeCaching(FILE_1, taintedIssue);

    verify(sourceLinesRepository).readLines(FILE_1);
    verifyNoMoreInteractions(sourceLinesRepository);
    DbIssues.Locations locations = taintedIssue.getLocations();
    assertThat(locations.getFlow(0).getLocation(0).getChecksum()).isEqualTo(DigestUtils.md5Hex("Stringstring='line-in-the-main-file';"));
    assertThat(locations.getFlow(0).getLocation(1).getChecksum()).isEmpty();
  }

  @Test
  public void beforeCaching_whenMultipleLocationsInMultipleFiles_shouldComputeAllChecksums() {
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

    executeBeforeCaching(FILE_1, taintedIssue);

    DbIssues.Locations locations = taintedIssue.getLocations();

    assertThat(locations.getFlow(0).getLocation(0).getChecksum()).isEqualTo(DigestUtils.md5Hex("Stringstring='line-in-the-main-file';"));
    assertThat(locations.getFlow(0).getLocation(1).getChecksum()).isEqualTo(DigestUtils.md5Hex("Stringstring='line-in-the-another-file';"));
  }

  @Test
  public void beforeCaching_whenMultipleLocationsInSameFile_shouldComputeAllChecksums() {
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

    executeBeforeCaching(FILE_1, taintedIssue);

    DbIssues.Locations locations = taintedIssue.getLocations();

    assertThat(locations.getFlow(0).getLocation(0).getChecksum()).isEqualTo(DigestUtils.md5Hex("Stringstring='line-in-the-main-file';"));
    assertThat(locations.getFlow(0).getLocation(1).getChecksum()).isEqualTo(DigestUtils.md5Hex("Stringstring='another-line-in-the-main-file';"));
  }

  @Test
  public void beforeCaching_whenNotTaintedIssue_shouldNotComputeChecksumForSecondaryLocations() {
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

    executeBeforeCaching(FILE_1, notTaintedIssue);

    DbIssues.Locations locations = notTaintedIssue.getLocations();
    assertLocationHashIsMadeOf(notTaintedIssue, "Stringstring='line-in-the-main-file';");
    assertThat(locations.getFlow(0).getLocation(0).getChecksum()).isEmpty();
    assertThat(locations.getFlow(0).getLocation(1).getChecksum()).isEmpty();
  }

  private void executeBeforeCaching(Component component, DefaultIssue issue) {
    underTest.beforeComponent(component);
    underTest.onIssue(component, issue);
    underTest.beforeCaching(component);
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
