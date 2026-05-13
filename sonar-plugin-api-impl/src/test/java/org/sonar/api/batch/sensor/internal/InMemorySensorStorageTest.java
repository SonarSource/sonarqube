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
package org.sonar.api.batch.sensor.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.issue.IssueResolution;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueResolution;
import org.sonar.api.rule.RuleKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.MapEntry.entry;

public class InMemorySensorStorageTest {

  InMemorySensorStorage underTest = new InMemorySensorStorage();

  @Test
  public void test_storeProperty() {
    assertThat(underTest.contextProperties).isEmpty();

    underTest.storeProperty("foo", "bar");
    assertThat(underTest.contextProperties).containsOnly(entry("foo", "bar"));
  }

  @Test
  public void storeProperty_throws_IAE_if_key_is_null() {
    assertThatThrownBy(() -> underTest.storeProperty(null, "bar"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Key of context property must not be null");
  }

  @Test
  public void storeProperty_throws_IAE_if_value_is_null() {
    assertThatThrownBy(() -> underTest.storeProperty("foo", null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of context property must not be null");
  }

  @Test
  public void test_storeAnalysisData() {
    // Given
    String key = "analysisKey";
    String mimeType = "mimeType";
    String dataString = "analysisData";
    ByteArrayInputStream dataStream = new ByteArrayInputStream(dataString.getBytes(StandardCharsets.UTF_8));

    // When
    underTest.storeAnalysisData(key, mimeType, dataStream);

    // Then
    assertThat(underTest.analysisDataEntries).containsKey(key);
    assertThat(new String(underTest.analysisDataEntries.get(key).data(), StandardCharsets.UTF_8)).isEqualTo(dataString);
  }

  @Test
  public void storeAnalysisData_throws_UOE_if_operation_not_supported() {
    underTest.storeAnalysisData("unsupportedKey", "mimeType", new ByteArrayInputStream("dummyData".getBytes(StandardCharsets.UTF_8)));
    ByteArrayInputStream dataStream = new ByteArrayInputStream("newData".getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> underTest.storeAnalysisData("unsupportedKey", "mimeType", dataStream))
      .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  public void storeAnalysisData_throws_IOE_on_data_handling_error() {
    InputStream faultyStream = new InputStream() {
      @Override
      public int read() throws IOException {
        throw new IOException("Simulated IO Exception");
      }
    };

    assertThatThrownBy(() -> underTest.storeAnalysisData("validKey", "mimeType", faultyStream))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Failed to read data from InputStream");
  }

  @Test
  public void test_storeIssueResolution() {
    assertThat(underTest.issueResolutionsByComponent).isEmpty();

    InputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.java")
      .setContents("class Foo {}")
      .build();
    DefaultIssueResolution resolution = new DefaultIssueResolution(underTest);
    resolution
      .forRules(Set.of(RuleKey.of("java", "S123")))
      .on(inputFile)
      .at(inputFile.selectLine(1))
      .comment("comment")
      .status(IssueResolution.Status.DEFAULT)
      .save();

    assertThat(underTest.issueResolutionsByComponent).containsKey("foo:src/Foo.java");
    List<IssueResolution> stored = underTest.issueResolutionsByComponent.get("foo:src/Foo.java");
    assertThat(stored).hasSize(1);
    assertThat(stored.get(0).ruleKeys()).containsExactly(RuleKey.of("java", "S123"));
    assertThat(stored.get(0).inputFile()).isEqualTo(inputFile);
    assertThat(stored.get(0).comment()).isEqualTo("comment");
    assertThat(stored.get(0).status()).isEqualTo(IssueResolution.Status.DEFAULT);
  }

  @Test
  public void test_storeIssueResolution_defaultsStatusWhenNotProvided() {
    InputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.java")
      .setContents("class Foo {}")
      .build();
    DefaultIssueResolution resolution = new DefaultIssueResolution(underTest);
    resolution
      .forRules(Set.of(RuleKey.of("java", "S123")))
      .on(inputFile)
      .at(inputFile.selectLine(1))
      .comment("comment")
      .save();

    List<IssueResolution> stored = underTest.issueResolutionsByComponent.get("foo:src/Foo.java");
    assertThat(stored).hasSize(1);
    assertThat(stored.get(0).status()).isEqualTo(IssueResolution.Status.DEFAULT);
  }
}
