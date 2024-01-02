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
package org.sonar.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.test.TestUtils;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.core.test.Test.Fake;

public class ProtobufTest {


  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void only_utils() {
    assertThat(TestUtils.hasOnlyPrivateConstructors(Protobuf.class)).isTrue();
  }

  @Test
  public void read_file_fails_if_file_does_not_exist() throws Exception {
    assertThatThrownBy(() -> {
      File file = temp.newFile();
      FileUtils.forceDelete(file);
      Protobuf.read(file, Fake.parser());
    }).isInstanceOf(ContextException.class)
      .hasMessageContaining("Unable to read message");
  }

  @Test
  public void read_file_returns_empty_message_if_file_is_empty() throws Exception {
    File file = temp.newFile();
    Fake msg = Protobuf.read(file, Fake.parser());
    assertThat(msg).isNotNull();
    assertThat(msg.isInitialized()).isTrue();
  }

  @Test
  public void read_file_returns_message() throws Exception {
    File file = temp.newFile();
    Protobuf.write(Fake.getDefaultInstance(), file);
    Fake message = Protobuf.read(file, Fake.parser());
    assertThat(message).isNotNull();
    assertThat(message.isInitialized()).isTrue();
  }

  @Test
  public void fail_to_write_single_message() {
    assertThatThrownBy(() -> {
      File dir = temp.newFolder();
      Protobuf.write(Fake.getDefaultInstance(), dir);
    }).isInstanceOf(ContextException.class)
      .hasMessageContaining("Unable to write message");
  }

  @Test
  public void write_and_read_streams() throws Exception {
    File file = temp.newFile();

    Fake item1 = Fake.newBuilder().setLabel("one").setLine(1).build();
    Fake item2 = Fake.newBuilder().setLabel("two").build();
    Protobuf.writeStream(asList(item1, item2), file, false);

    CloseableIterator<Fake> it = Protobuf.readStream(file, Fake.parser());
    Fake read = it.next();
    assertThat(read.getLabel()).isEqualTo("one");
    assertThat(read.getLine()).isOne();
    read = it.next();
    assertThat(read.getLabel()).isEqualTo("two");
    assertThat(read.hasLine()).isFalse();
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  public void write_gzip_file() throws IOException {
    File file = temp.newFile();

    Fake item1 = Fake.newBuilder().setLabel("one").setLine(1).build();
    Protobuf.writeGzip(item1, file);
    try (InputStream is = new GZIPInputStream(new FileInputStream(file))) {
      assertThat(Protobuf.read(is, Fake.parser()).getLabel()).isEqualTo("one");
    }
  }

  @Test
  public void read_gzip_stream() throws IOException {
    File file = temp.newFile();

    Fake item1 = Fake.newBuilder().setLabel("one").setLine(1).build();
    Fake item2 = Fake.newBuilder().setLabel("two").setLine(2).build();

    try (OutputStream os = new GZIPOutputStream(new FileOutputStream(file))) {
      item1.writeDelimitedTo(os);
      item2.writeDelimitedTo(os);
    }

    Iterable<Fake> it = () -> Protobuf.readGzipStream(file, Fake.parser());

    assertThat(it).containsExactly(item1, item2);
  }

  @Test
  public void fail_to_read_stream() {
    assertThatThrownBy(() -> {
      File dir = temp.newFolder();
      Protobuf.readStream(dir, Fake.parser());
    }).isInstanceOf(ContextException.class)
      .hasMessageContaining("Unable to read messages");
  }

  @Test
  public void read_empty_stream() throws Exception {
    File file = temp.newFile();
    CloseableIterator<Fake> it = Protobuf.readStream(file, Fake.parser());
    assertThat(it).isNotNull();
    assertThat(it.hasNext()).isFalse();
  }
}
