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
package org.sonar.xoo.rule;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.IssueResolution;
import org.sonar.api.rule.RuleKey;
import org.sonar.xoo.Xoo;

import static org.assertj.core.api.Assertions.assertThat;

class IssueResolutionSensorTest {

  @TempDir
  private File temp;

  private final IssueResolutionSensor sensor = new IssueResolutionSensor();

  @Test
  void test_parse_accept_issue_resolution() throws IOException {
    DefaultInputFile inputFile = createInputFile("//sonar-resolve java:S899 Mr X allowed me\n");
    SensorContextTester context = createContext(inputFile);

    sensor.execute(context);

    List<IssueResolution> issueResolutions = getIssueResolutionsForFile(context, inputFile);
    assertThat(issueResolutions).hasSize(1);

    IssueResolution resolution = issueResolutions.get(0);
    assertThat(resolution.status()).isEqualTo(IssueResolution.Status.DEFAULT);
    assertThat(resolution.ruleKeys()).containsExactly(RuleKey.of("java", "S899"));
    assertThat(resolution.inputFile()).isEqualTo(inputFile);
    assertThat(resolution.textRange()).isNotNull();
    assertThat(resolution.textRange().start().line()).isEqualTo(1);
    assertThat(resolution.comment()).isEqualTo("Mr X allowed me");
  }

  @Test
  void test_parse_fp_issue_resolution() throws IOException {
    DefaultInputFile inputFile = createInputFile("//sonar-resolve [FP] java:S123 false positive\n");
    SensorContextTester context = createContext(inputFile);

    sensor.execute(context);

    List<IssueResolution> issueResolutions = getIssueResolutionsForFile(context, inputFile);
    assertThat(issueResolutions).hasSize(1);

    IssueResolution resolution = issueResolutions.get(0);
    assertThat(resolution.status()).isEqualTo(IssueResolution.Status.FALSE_POSITIVE);
    assertThat(resolution.ruleKeys()).containsExactly(RuleKey.of("java", "S123"));
    assertThat(resolution.inputFile()).isEqualTo(inputFile);
    assertThat(resolution.textRange()).isNotNull();
    assertThat(resolution.textRange().start().line()).isEqualTo(1);
    assertThat(resolution.comment()).isEqualTo("false positive");
  }

  @Test
  void test_parse_multiple_rule_keys() throws IOException {
    DefaultInputFile inputFile = createInputFile("//sonar-resolve java:S899,java:S1036 comment\n");
    SensorContextTester context = createContext(inputFile);

    sensor.execute(context);

    List<IssueResolution> issueResolutions = getIssueResolutionsForFile(context, inputFile);
    assertThat(issueResolutions).hasSize(1);

    IssueResolution resolution = issueResolutions.get(0);
    assertThat(resolution.ruleKeys()).containsExactlyInAnyOrder(
      RuleKey.of("java", "S899"),
      RuleKey.of("java", "S1036"));
    assertThat(resolution.comment()).isEqualTo("comment");
  }

  @Test
  void test_no_issue_resolution_when_no_sonar_resolve_comments() throws IOException {
    DefaultInputFile inputFile = createInputFile("some regular code\nno issue resolution here\n");
    SensorContextTester context = createContext(inputFile);

    sensor.execute(context);

    assertThat(context.getIssueResolutions()).isEmpty();
  }

  @Test
  void test_explicit_accept_status() throws IOException {
    DefaultInputFile inputFile = createInputFile("//sonar-resolve [ACCEPT] java:S899 comment\n");
    SensorContextTester context = createContext(inputFile);

    sensor.execute(context);

    List<IssueResolution> issueResolutions = getIssueResolutionsForFile(context, inputFile);
    assertThat(issueResolutions).hasSize(1);

    IssueResolution resolution = issueResolutions.get(0);
    assertThat(resolution.status()).isEqualTo(IssueResolution.Status.DEFAULT);
    assertThat(resolution.ruleKeys()).containsExactly(RuleKey.of("java", "S899"));
    assertThat(resolution.comment()).isEqualTo("comment");
  }

  private static List<IssueResolution> getIssueResolutionsForFile(SensorContextTester context, DefaultInputFile inputFile) {
    return context.getIssueResolutions().getOrDefault(inputFile.key(), Collections.emptyList());
  }

  private DefaultInputFile createInputFile(String content) throws IOException {
    Path baseDir = temp.toPath().toAbsolutePath();
    Path srcDir = baseDir.resolve("src");
    Files.createDirectories(srcDir);
    Path sourceFile = srcDir.resolve("Foo.xoo");
    Files.writeString(sourceFile, content, StandardCharsets.UTF_8);

    return new TestInputFileBuilder("foo", "src/Foo.xoo")
      .setLanguage(Xoo.KEY)
      .setType(InputFile.Type.MAIN)
      .setModuleBaseDir(baseDir)
      .setCharset(StandardCharsets.UTF_8)
      .initMetadata(content)
      .build();
  }

  private SensorContextTester createContext(DefaultInputFile inputFile) {
    SensorContextTester context = SensorContextTester.create(temp.toPath().toAbsolutePath());
    context.fileSystem().add(inputFile);
    context.settings().setProperty("sonar.issueresolutionsensor.activate", "true");
    return context;
  }
}
