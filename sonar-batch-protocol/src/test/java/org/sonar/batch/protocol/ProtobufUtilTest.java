/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.protocol;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.test.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ProtobufUtilTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void only_utils() {
    assertThat(TestUtils.hasOnlyPrivateConstructors(ProtobufUtil.class));
  }

  @Test
  public void readFile_fails_if_file_does_not_exist() throws Exception {
    thrown.expect(IllegalStateException.class);

    File file = temp.newFile();
    FileUtils.forceDelete(file);
    ProtobufUtil.readFile(file, BatchReport.Metadata.PARSER);
  }

  @Test
  public void readFile_returns_empty_message_if_file_is_empty() throws Exception {
    File file = temp.newFile();
    BatchReport.Metadata msg = ProtobufUtil.readFile(file, BatchReport.Metadata.PARSER);
    assertThat(msg).isNotNull();
    assertThat(msg.isInitialized()).isTrue();
  }

  @Test
  public void readFile_returns_message() throws Exception {
    File file = temp.newFile();
    ProtobufUtil.writeToFile(BatchReport.Metadata.getDefaultInstance(), file);
    BatchReport.Metadata message = ProtobufUtil.readFile(file, BatchReport.Metadata.PARSER);
    assertThat(message).isNotNull();
    assertThat(message.isInitialized()).isTrue();
  }
}
