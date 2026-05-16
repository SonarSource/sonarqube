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
package org.sonar.ce.task.util;

import com.sonarsource.governance.projectdump.protobuf.ProjectDump.Metadata;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.ce.task.util.Files2.FILES2;

public class Protobuf2Test {

  private static final String PROJECT_KEY_1 = "foo";
  private static final String PROJECT_KEY_2 = "bar";

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void write_to_and_parse_from_file() throws Exception {
    File file = temp.newFile();
    try (FileOutputStream output = FILES2.openOutputStream(file, false)) {
      Protobuf2.writeTo(newMetadata(PROJECT_KEY_1), output);
    }
    try (FileInputStream input = FILES2.openInputStream(file)) {
      Metadata metadata = Protobuf2.parseFrom(Metadata.parser(), input);
      assertThat(metadata.getProjectKey()).isEqualTo(PROJECT_KEY_1);
    }
  }

  @Test
  public void write_to_and_parse_delimited_from_file() throws Exception {
    File file = temp.newFile();
    try (FileOutputStream output = FILES2.openOutputStream(file, false)) {
      Protobuf2.writeDelimitedTo(newMetadata(PROJECT_KEY_1), output);
      Protobuf2.writeDelimitedTo(newMetadata(PROJECT_KEY_2), output);
    }
    try (FileInputStream input = FILES2.openInputStream(file)) {
      assertThat(Protobuf2.parseDelimitedFrom(Metadata.parser(), input).getProjectKey()).isEqualTo(PROJECT_KEY_1);
      assertThat(Protobuf2.parseDelimitedFrom(Metadata.parser(), input).getProjectKey()).isEqualTo(PROJECT_KEY_2);
      assertThat(Protobuf2.parseDelimitedFrom(Metadata.parser(), input)).isNull();
    }
  }

  @Test
  public void writeTo_throws_ISE_on_error() throws Exception {
    try (FailureOutputStream output = new FailureOutputStream()) {
      assertThatThrownBy(() -> Protobuf2.writeTo(newMetadata(PROJECT_KEY_1), output))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Can not write message");
    }
  }

  @Test
  public void writeDelimitedTo_throws_ISE_on_error() throws Exception {
    try (FailureOutputStream output = new FailureOutputStream()) {
      assertThatThrownBy(() -> Protobuf2.writeDelimitedTo(newMetadata(PROJECT_KEY_1), output))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Can not write message");
    }
  }

  @Test
  public void parseFrom_throws_ISE_on_error() throws Exception {
    try (FailureInputStream input = new FailureInputStream()) {
      assertThatThrownBy(() -> Protobuf2.parseFrom(Metadata.parser(), input))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Can not parse message");
    }
  }

  @Test
  public void parseDelimitedFrom_throws_ISE_on_error() throws Exception {
    try (FailureInputStream input = new FailureInputStream()) {
      assertThatThrownBy(() -> Protobuf2.parseDelimitedFrom(Metadata.parser(), input))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Can not parse message");
    }
  }

  private static Metadata newMetadata(String projectKey) {
    return Metadata.newBuilder().setProjectKey(projectKey).build();
  }

  private static class FailureOutputStream extends OutputStream {
    @Override
    public void write(int b) throws IOException {
      throw new IOException("Failure");
    }
  }

  private static class FailureInputStream extends InputStream {
    @Override
    public int read() throws IOException {
      throw new IOException("failure");
    }
  }
}
