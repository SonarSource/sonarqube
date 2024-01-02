/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
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
  private static final String LINE_IN_THE_MAIN_FILE = "String string = 'line-in-the-main-file';";
  private static final String ANOTHER_LINE_IN_THE_MAIN_FILE = "String string = 'another-line-in-the-main-file';";
  private static final String LINE_IN_ANOTHER_FILE = "String string = 'line-in-the-another-file';";

  private static final RuleKey RULE_KEY = RuleKey.of("javasecurity", "S001");
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
  private final ComputeLocationHashesVisitor underTest = new ComputeLocationHashesVisitor(taintChecker, sourceLinesRepository, treeRootHolder);

  @Before
  public void before() {
    Iterator<String> stringIterator = IntStream.rangeClosed(1, 9)
      .mapToObj(i -> String.format(EXAMPLE_LINE_OF_CODE_FORMAT, i))
      .iterator();
    when(sourceLinesRepository.readLines(FILE_1)).thenReturn(CloseableIterator.from(stringIterator));
    when(sourceLinesRepository.readLines(FILE_2)).thenReturn(newOneLineIterator(LINE_IN_ANOTHER_FILE));
    treeRootHolder.setRoot(ROOT);
  }

  @Test
  public void do_nothing_if_issue_is_unchanged() {
    DefaultIssue issue = createIssue()
      .setLocationsChanged(false)
      .setNew(false)
      .setLocations(DbIssues.Locations.newBuilder().setTextRange(createRange(1, 0, 3, EXAMPLE_LINE_OF_CODE_FORMAT.length() - 1)).build());

    underTest.beforeComponent(FILE_1);
    underTest.onIssue(FILE_1, issue);
    underTest.beforeCaching(FILE_1);

    DbIssues.Locations locations = issue.getLocations();
    assertThat(locations.getChecksum()).isEmpty();
    verifyNoInteractions(sourceLinesRepository);
  }

  @Test
  public void do_nothing_if_issue_is_not_taint_vulnerability() {
    DefaultIssue issue = createIssue()
      .setRuleKey(RuleKey.of("repo", "rule"))
      .setLocations(DbIssues.Locations.newBuilder().setTextRange(createRange(1, 0, 3, EXAMPLE_LINE_OF_CODE_FORMAT.length() - 1)).build());

    underTest.onIssue(FILE_1, issue);
    underTest.beforeCaching(FILE_1);

    DbIssues.Locations locations = issue.getLocations();
    assertThat(locations.getChecksum()).isEmpty();
    verifyNoInteractions(sourceLinesRepository);
  }

  @Test
  public void calculates_hash_for_multiple_lines() {
    DefaultIssue issue = createIssue()
      .setLocations(DbIssues.Locations.newBuilder().setTextRange(createRange(1, 0, 3, EXAMPLE_LINE_OF_CODE_FORMAT.length() - 1)).build());

    underTest.onIssue(FILE_1, issue);
    underTest.beforeCaching(FILE_1);

    assertLocationHashIsMadeOf(issue, "intexample=line+of+code+1;intexample=line+of+code+2;intexample=line+of+code+3;");
  }

  @Test
  public void calculates_hash_for_multiple_files() {
    DefaultIssue issue1 = createIssue()
      .setLocations(DbIssues.Locations.newBuilder().setTextRange(createRange(1, 0, 3, EXAMPLE_LINE_OF_CODE_FORMAT.length() - 1)).build());
    DefaultIssue issue2 = createIssue()
      .setLocations(DbIssues.Locations.newBuilder().setTextRange(createRange(1, 0, 1, LINE_IN_ANOTHER_FILE.length())).build());

    underTest.onIssue(FILE_1, issue1);
    underTest.beforeCaching(FILE_1);

    underTest.onIssue(FILE_2, issue2);
    underTest.beforeCaching(FILE_2);

    assertLocationHashIsMadeOf(issue1, "intexample=line+of+code+1;intexample=line+of+code+2;intexample=line+of+code+3;");
    assertLocationHashIsMadeOf(issue2, "Stringstring='line-in-the-another-file';");
  }

  @Test
  public void calculates_hash_for_partial_line() {
    DefaultIssue issue = createIssue()
      .setLocations(DbIssues.Locations.newBuilder().setTextRange(createRange(1, 13, 1, EXAMPLE_LINE_OF_CODE_FORMAT.length() - 1)).build());

    underTest.onIssue(FILE_1, issue);
    underTest.beforeCaching(FILE_1);

    assertLocationHashIsMadeOf(issue, "line+of+code+1;");
  }

  @Test
  public void calculates_hash_for_partial_multiple_lines() {
    DefaultIssue issue = createIssue()
      .setLocations(DbIssues.Locations.newBuilder().setTextRange(createRange(1, 13, 3, 11)).build());

    underTest.onIssue(FILE_1, issue);
    underTest.beforeCaching(FILE_1);

    assertLocationHashIsMadeOf(issue, "line+of+code+1;intexample=line+of+code+2;intexample");
  }

  @Test
  public void dont_calculate_hash_if_no_textRange() {
    // primary location and one of the secondary locations have no text range
    DefaultIssue issue = createIssue()
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

    underTest.onIssue(FILE_1, issue);
    underTest.beforeCaching(FILE_1);

    verify(sourceLinesRepository).readLines(FILE_1);
    verifyNoMoreInteractions(sourceLinesRepository);
    DbIssues.Locations locations = issue.getLocations();
    assertThat(locations.getFlow(0).getLocation(0).getChecksum()).isEqualTo(DigestUtils.md5Hex("Stringstring='line-in-the-main-file';"));
    assertThat(locations.getFlow(0).getLocation(1).getChecksum()).isEmpty();
  }

  @Test
  public void calculates_hash_for_multiple_locations() {
    DefaultIssue issue = createIssue()
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

    underTest.onIssue(FILE_1, issue);
    underTest.beforeCaching(FILE_1);

    DbIssues.Locations locations = issue.getLocations();

    assertThat(locations.getFlow(0).getLocation(0).getChecksum()).isEqualTo(DigestUtils.md5Hex("Stringstring='line-in-the-main-file';"));
    assertThat(locations.getFlow(0).getLocation(1).getChecksum()).isEqualTo(DigestUtils.md5Hex("Stringstring='line-in-the-another-file';"));
  }

  @Test
  public void calculates_hash_for_multiple_locations_in_same_file() {
    DefaultIssue issue = createIssue()
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

    underTest.onIssue(FILE_1, issue);
    underTest.beforeCaching(FILE_1);

    DbIssues.Locations locations = issue.getLocations();

    assertThat(locations.getFlow(0).getLocation(0).getChecksum()).isEqualTo(DigestUtils.md5Hex("Stringstring='line-in-the-main-file';"));
    assertThat(locations.getFlow(0).getLocation(1).getChecksum()).isEqualTo(DigestUtils.md5Hex("Stringstring='another-line-in-the-main-file';"));
  }

  private DbCommons.TextRange createRange(int startLine, int startOffset, int endLine, int endOffset) {
    return DbCommons.TextRange.newBuilder()
      .setStartLine(startLine).setStartOffset(startOffset)
      .setEndLine(endLine).setEndOffset(endOffset)
      .build();
  }

  private DefaultIssue createIssue() {
    return new DefaultIssue()
      .setLocationsChanged(true)
      .setRuleKey(RULE_KEY)
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
