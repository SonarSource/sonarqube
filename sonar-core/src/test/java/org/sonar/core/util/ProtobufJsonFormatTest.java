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
package org.sonar.core.util;

import com.google.protobuf.ByteString;
import java.io.StringWriter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.text.JsonWriter;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class ProtobufJsonFormatTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void convert_protobuf_to_json() throws Exception {
    org.sonar.core.test.Test.Fake protobuf = org.sonar.core.test.Test.Fake.newBuilder()
      .setAString("foo")
      .setAnInt(10)
      .setALong(100L)
      .setABoolean(true)
      .setADouble(3.14)
      .setAnEnum(org.sonar.core.test.Test.FakeEnum.GREEN)
      .addAllAnArray(asList("one", "two", "three"))
      .setANestedMessage(org.sonar.core.test.Test.NestedFake.newBuilder().setLabel("bar").build())
      .build();

    StringWriter json = new StringWriter();
    JsonWriter jsonWriter = JsonWriter.of(json);
    ProtobufJsonFormat.write(protobuf, jsonWriter);

    assertThat(json.toString())
      .isEqualTo("{\"aString\":\"foo\",\"anInt\":10,\"aLong\":100,\"aDouble\":3.14,\"aBoolean\":true,\"anEnum\":\"GREEN\",\"anArray\":[\"one\",\"two\",\"three\"],\"aNestedMessage\":{\"label\":\"bar\"}}");
  }

  @Test
  public void protobuf_bytes_field_can_not_be_converted_to_json() throws Exception {
    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("JSON format does not support the binary field 'someBytes'");

    org.sonar.core.test.Test.Fake protobuf = org.sonar.core.test.Test.Fake.newBuilder()
      .setSomeBytes(ByteString.copyFrom(new byte[]{2, 4}))
      .build();

    ProtobufJsonFormat.write(protobuf, JsonWriter.of(new StringWriter()));
  }

  @Test
  public void protobuf_empty_strings_are_not_output() throws Exception {
    org.sonar.core.test.Test.Fake protobuf = org.sonar.core.test.Test.Fake.newBuilder().build();

    // field is not set but value is "", not null
    assertThat(protobuf.hasAString()).isFalse();
    assertThat(protobuf.getAString()).isEqualTo("");

    StringWriter json = new StringWriter();
    JsonWriter jsonWriter = JsonWriter.of(json);
    ProtobufJsonFormat.write(protobuf, jsonWriter);
    assertThat(json.toString()).isEqualTo("{}");
  }
}
