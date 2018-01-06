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
package org.sonar.core.util;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.test.TestUtils;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.test.Test.Fake;
import static org.sonar.core.util.Protobuf.setNullable;

public class ProtobufTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void only_utils() {
    assertThat(TestUtils.hasOnlyPrivateConstructors(Protobuf.class)).isTrue();
  }

  @Test
  public void read_file_fails_if_file_does_not_exist() throws Exception {
    thrown.expect(ContextException.class);
    thrown.expectMessage("Unable to read message");

    File file = temp.newFile();
    FileUtils.forceDelete(file);
    Protobuf.read(file, Fake.parser());
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
  public void fail_to_write_single_message() throws Exception {
    thrown.expect(ContextException.class);
    thrown.expectMessage("Unable to write message");

    File dir = temp.newFolder();
    Protobuf.write(Fake.getDefaultInstance(), dir);
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
    assertThat(read.getLine()).isEqualTo(1);
    read = it.next();
    assertThat(read.getLabel()).isEqualTo("two");
    assertThat(read.hasLine()).isFalse();
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  public void fail_to_read_stream() throws Exception {
    thrown.expect(ContextException.class);
    thrown.expectMessage("Unable to read messages");

    File dir = temp.newFolder();
    Protobuf.readStream(dir, Fake.parser());
  }

  @Test
  public void read_empty_stream() throws Exception {
    File file = temp.newFile();
    CloseableIterator<Fake> it = Protobuf.readStream(file, Fake.parser());
    assertThat(it).isNotNull();
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  public void setNullable_sets_field_if_value_is_not_null() {
    Fake.Builder builder = Fake.newBuilder();

    setNullable("foo", builder::setLabel);
    assertThat(builder.getLabel()).isEqualTo("foo");

    builder.clear();
    setNullable(null, builder::setLabel);
    assertThat(builder.hasLabel()).isFalse();
  }

  @Test
  public void setNullable_converts_value_and_sets_field_if_value_is_not_null() {
    Fake.Builder builder = Fake.newBuilder();

    setNullable("foo", builder::setLabel, StringUtils::upperCase);
    assertThat(builder.getLabel()).isEqualTo("FOO");

    builder.clear();
    setNullable((String)null, builder::setLabel, StringUtils::upperCase);
    assertThat(builder.hasLabel()).isFalse();

    // do not set field if value is present but result of conversion is null
    builder.clear();
    setNullable("    ", builder::setLabel, StringUtils::trimToNull);
    assertThat(builder.hasLabel()).isFalse();
  }
}
