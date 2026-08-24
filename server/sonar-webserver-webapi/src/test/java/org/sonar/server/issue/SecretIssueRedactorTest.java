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
package org.sonar.server.issue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.protobuf.DbIssues;

import static org.assertj.core.api.Assertions.assertThat;

class SecretIssueRedactorTest {

  @Test
  void redactSourceLines_whenIssueHasTrustedRange_shouldRedactReportedRange() {
    IssueDto issue = secretIssue("secrets", "S1001", 1, 6, 12);
    List<DbFileSources.Line> lines = List.of(line(1, "token=secret-value"), line(2, "safe"));

    List<DbFileSources.Line> result = SecretIssueRedactor.redactSourceLines(lines, List.of(issue));

    assertThat(result).extracting(DbFileSources.Line::getSource).containsExactly("token=******-value", "safe");
  }

  @Test
  void redactSourceLines_whenTrustedRangeSpansMultipleLines_shouldRedactRangeAndPreserveLineLengths() {
    IssueDto issue = secretIssue("secrets", "S1001", 1, 2, 3, 2);
    List<DbFileSources.Line> lines = List.of(line(1, "aa-secret"), line(2, "secret-bb"), line(3, "cc-secret-dd"));

    List<DbFileSources.Line> result = SecretIssueRedactor.redactSourceLines(lines, List.of(issue));

    assertThat(result).extracting(DbFileSources.Line::getSource).containsExactly("aa*******", "*********", "**-secret-dd");
    assertThat(result).extracting(line -> line.getSource().length()).containsExactly(9, 9, 12);
  }

  @Test
  void redactSourceLines_whenIssueRuleIsNotScoped_shouldNotRedactSource() {
    IssueDto issue = secretIssue("java", "S100", 1, 0, 1);
    List<DbFileSources.Line> lines = List.of(line(1, "token=secret"), line(2, "safe"));

    List<DbFileSources.Line> result = SecretIssueRedactor.redactSourceLines(lines, List.of(issue));

    assertThat(result).extracting(DbFileSources.Line::getSource).containsExactly("token=secret", "safe");
  }

  @Test
  void redactSourceLines_whenIssueIsS2115_shouldNotRedactSource() {
    IssueDto issue = secretIssue("java", "S2115", 1, 0, 1);
    List<DbFileSources.Line> lines = List.of(line(1, "password="), line(2, "safe"));

    List<DbFileSources.Line> result = SecretIssueRedactor.redactSourceLines(lines, List.of(issue));

    assertThat(result).extracting(DbFileSources.Line::getSource).containsExactly("password=", "safe");
  }

  @Test
  void redactSourceLines_whenTrustedSecretIssueHasNoTextRange_shouldRedactEntireReturnedSource() {
    IssueDto issue = new IssueDto().setRuleKey("secrets", "S1001").setLine(1);

    List<DbFileSources.Line> result = SecretIssueRedactor.redactSourceLines(List.of(line(1, "token=secret"), line(2, "safe")), List.of(issue));

    assertThat(result).extracting(DbFileSources.Line::getSource).containsExactly("************", "****");
  }

  @Test
  void redactSourceLines_whenTrustedSecretRangeHasNoOffsets_shouldRedactEntireReturnedSource() {
    DbCommons.TextRange textRange = DbCommons.TextRange.newBuilder().setStartLine(1).setEndLine(1).build();
    IssueDto issue = new IssueDto()
      .setRuleKey("secrets", "S1001")
      .setLine(1)
      .setLocations(DbIssues.Locations.newBuilder().setTextRange(textRange).build());

    List<DbFileSources.Line> result = SecretIssueRedactor.redactSourceLines(List.of(line(1, "token=secret"), line(2, "safe")), List.of(issue));

    assertThat(result).extracting(DbFileSources.Line::getSource).containsExactly("************", "****");
  }

  @Test
  void redactSourceLines_whenTrustedRangesOverlap_shouldRedactTheirUnion() {
    IssueDto firstIssue = secretIssue("secrets", "S1001", 1, 2, 6);
    IssueDto secondIssue = secretIssue("secrets", "S1002", 1, 4, 9);

    List<DbFileSources.Line> result = SecretIssueRedactor.redactSourceLines(List.of(line(1, "aa-secret-value")), List.of(firstIssue, secondIssue));

    assertThat(result.get(0).getSource()).isEqualTo("aa*******-value");
  }

  @Test
  void redactSourceLines_whenTrustedSecretRangeIsInvalid_shouldRedactEntireReturnedSource() {
    DbCommons.TextRange textRange = DbCommons.TextRange.newBuilder().setStartLine(0).setStartOffset(0).setEndLine(1).setEndOffset(1).build();
    IssueDto issue = new IssueDto()
      .setRuleKey("secrets", "S1001")
      .setLine(1)
      .setLocations(DbIssues.Locations.newBuilder().setTextRange(textRange).build());

    List<DbFileSources.Line> result = SecretIssueRedactor.redactSourceLines(List.of(line(1, "token=secret"), line(2, "safe")), List.of(issue));

    assertThat(result).extracting(DbFileSources.Line::getSource).containsExactly("************", "****");
  }

  @Test
  void redactSourceLines_whenIssueIsS2068_shouldRedactEntireReturnedSource() {
    IssueDto issue = secretIssue("java", "S2068", 1, 30, 38);
    String source = "  private static final String password = \"not-a-real-secret-xyZ123\";";

    List<DbFileSources.Line> result = SecretIssueRedactor.redactSourceLines(List.of(line(1, source), line(2, "safe")), List.of(issue));

    assertThat(result).extracting(DbFileSources.Line::getSource).containsExactly("  " + "*".repeat(source.length() - 2), "****");
  }

  @Test
  void redactSourceLines_whenIssueIsFailClosedRule_shouldRedactEntireReturnedSource() {
    List<DbFileSources.Line> lines = List.of(line(1, "password=secret"), line(2, "safe"));

    for (IssueDto issue : List.of(
      secretIssue("java", "S2068", 1, 0, 8),
      secretIssue("python", "S6418", 1, 0, 6),
      secretIssue("docker", "S6472", 1, 0, 10),
      secretIssue("python", "S6779", 1, 0, 10),
      secretIssue("python", "S6781", 1, 0, 10),
      secretIssue("java", "S6437", 1, 0, 8))) {
      List<DbFileSources.Line> result = SecretIssueRedactor.redactSourceLines(lines, List.of(issue));

      assertThat(result).extracting(DbFileSources.Line::getSource).containsExactly("***************", "****");
    }
  }

  @Test
  void redactSourceLines_whenS6437HasRelevantFlowLocation_shouldRedactPrimaryAndRelevantFlowLines() {
    IssueDto issue = s6437Issue("file-uuid", textRange(4, 8, 4, 27), flowLocation("file-uuid", textRange(2, 4, 2, 33)));
    List<DbFileSources.Line> lines = List.of(
      line(1, "public final class Demo {"),
      line(2, "    static final String VALUE = \"not-a-real-password\";"),
      line(3, ""),
      line(4, "  use(VALUE.toCharArray());"),
      line(5, "}"));

    List<DbFileSources.Line> result = SecretIssueRedactor.redactSourceLines(lines, List.of(issue));

    assertThat(result).extracting(DbFileSources.Line::getSource).containsExactly(
      "public final class Demo {",
      "    " + "*".repeat(lines.get(1).getSource().length() - 4),
      "",
      "  " + "*".repeat(lines.get(3).getSource().length() - 2),
      "}");
  }

  @Test
  void redactSourceLines_whenS6437HasNoRelevantFlowLocation_shouldRedactEntireReturnedSource() {
    IssueDto issue = s6437Issue("file-uuid", textRange(2, 4, 2, 12));
    List<DbFileSources.Line> lines = List.of(line(1, "String VALUE = \"not-a-real-password\";"), line(2, "use(VALUE);"));

    List<DbFileSources.Line> result = SecretIssueRedactor.redactSourceLines(lines, List.of(issue));

    assertThat(result).extracting(DbFileSources.Line::getSource).containsExactly(
      "*".repeat(lines.get(0).getSource().length()),
      "*".repeat(lines.get(1).getSource().length()));
  }

  @Test
  void redactSourceLines_whenS6437OnlyHasCrossComponentFlowLocation_shouldRedactEntireReturnedSource() {
    IssueDto issue = s6437Issue("use-file-uuid", textRange(2, 4, 2, 12), flowLocation("definition-file-uuid", textRange(2, 4, 2, 33)));
    List<DbFileSources.Line> lines = List.of(line(1, "String VALUE = \"not-a-real-password\";"), line(2, "use(VALUE);"));

    List<DbFileSources.Line> result = SecretIssueRedactor.redactSourceLines(lines, List.of(issue));

    assertThat(result).extracting(DbFileSources.Line::getSource).containsExactly(
      "*".repeat(lines.get(0).getSource().length()),
      "*".repeat(lines.get(1).getSource().length()));
  }

  private static IssueDto secretIssue(String repository, String ruleKey, int line, int startOffset, int endOffset) {
    return secretIssue(repository, ruleKey, line, startOffset, line, endOffset);
  }

  private static IssueDto secretIssue(String repository, String ruleKey, int startLine, int startOffset, int endLine, int endOffset) {
    DbCommons.TextRange textRange = textRange(startLine, startOffset, endLine, endOffset);
    return new IssueDto()
      .setRuleKey(repository, ruleKey)
      .setLine(startLine)
      .setLocations(DbIssues.Locations.newBuilder().setTextRange(textRange).build());
  }

  private static IssueDto s6437Issue(String componentUuid, DbCommons.TextRange primaryLocation, DbIssues.Location... flowLocations) {
    return new IssueDto()
      .setComponentUuid(componentUuid)
      .setRuleKey("java", "S6437")
      .setLine(primaryLocation.getStartLine())
      .setLocations(DbIssues.Locations.newBuilder()
        .setTextRange(primaryLocation)
        .addFlow(DbIssues.Flow.newBuilder().addAllLocation(List.of(flowLocations)))
        .build());
  }

  private static DbIssues.Location flowLocation(String componentUuid, DbCommons.TextRange textRange) {
    return DbIssues.Location.newBuilder().setComponentId(componentUuid).setTextRange(textRange).build();
  }

  private static DbCommons.TextRange textRange(int startLine, int startOffset, int endLine, int endOffset) {
    return DbCommons.TextRange.newBuilder()
      .setStartLine(startLine)
      .setStartOffset(startOffset)
      .setEndLine(endLine)
      .setEndOffset(endOffset)
      .build();
  }

  private static DbFileSources.Line line(int line, String source) {
    return DbFileSources.Line.newBuilder().setLine(line).setSource(source).build();
  }
}
