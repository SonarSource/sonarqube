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
import org.sonar.core.test.Test.JsonArrayTest;
import org.sonar.core.test.Test.JsonTest;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.util.ProtobufJsonFormat.toJson;

public class ProtobufJsonFormatTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void convert_protobuf_to_json() {
    JsonTest protobuf = JsonTest.newBuilder()
      .setStringField("foo")
      .setIntField(10)
      .setLongField(100L)
      .setDoubleField(3.14)
      .setBooleanField(true)
      .setEnumField(org.sonar.core.test.Test.FakeEnum.GREEN)
      .addAllAnArray(asList("one", "two", "three"))
      .setNested(org.sonar.core.test.Test.NestedJsonTest.newBuilder().setLabel("bar").build())
      .build();

    assertThat(toJson(protobuf))
      .isEqualTo(
        "{\"stringField\":\"foo\",\"intField\":10,\"longField\":100,\"doubleField\":3.14,\"booleanField\":true,\"enumField\":\"GREEN\",\"nested\":{\"label\":\"bar\"},\"anArray\":[\"one\",\"two\",\"three\"]}");
  }

  @Test
  public void protobuf_bytes_field_can_not_be_converted_to_json() {
    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("JSON format does not support type 'BYTE_STRING' of field 'bytesField'");

    JsonTest protobuf = JsonTest.newBuilder()
      .setBytesField(ByteString.copyFrom(new byte[]{2, 4}))
      .build();

    ProtobufJsonFormat.write(protobuf, JsonWriter.of(new StringWriter()));
  }

  @Test
  public void protobuf_absent_fields_are_not_output() {
    JsonTest msg = JsonTest.newBuilder().build();

    // fields are absent
    assertThat(msg.hasStringField()).isFalse();
    assertThat(msg.hasIntField()).isFalse();

    // the repeated field "anArray" is always present. This is the standard behavior of protobuf. It
    // does not make the difference between null and empty arrays.
    assertThat(toJson(msg)).isEqualTo("{\"anArray\":[]}");
  }

  @Test
  public void protobuf_present_and_empty_string_field_is_output() {
    JsonTest msg = JsonTest.newBuilder().setStringField("").build();

    // field is present
    assertThat(msg.hasStringField()).isTrue();
    assertThat(msg.getStringField()).isEqualTo("");

    assertThat(toJson(msg)).contains("\"stringField\":\"\"");
  }


  @Test
  public void protobuf_empty_array_marked_as_present_is_output() {
    JsonArrayTest msg = JsonArrayTest.newBuilder()
      .setANullableArrayPresentIfEmpty(true)
      .build();

    // repeated field "aNullableArray" is marked as present through the boolean field "aNullableArrayPresentIfEmpty"
    assertThat(msg.hasANullableArrayPresentIfEmpty()).isTrue();
    assertThat(msg.getANullableArrayPresentIfEmpty()).isTrue();

    // JSON contains the repeated field, but not the boolean marker field
    assertThat(toJson(msg)).isEqualTo("{\"aNullableArray\":[]}");
  }

  @Test
  public void protobuf_empty_array_marked_as_absent_is_not_output() {
    JsonArrayTest msg = JsonArrayTest.newBuilder()
      .setANullableArrayPresentIfEmpty(false)
      .build();

    // repeated field "aNullableArray" is marked as absent through the boolean field "aNullableArrayPresentIfEmpty"
    assertThat(msg.hasANullableArrayPresentIfEmpty()).isTrue();
    assertThat(msg.getANullableArrayPresentIfEmpty()).isFalse();

    // JSON does not contain the array nor the boolean marker
    assertThat(toJson(msg)).isEqualTo("{}");
  }

  @Test
  public void protobuf_non_empty_array_is_output_even_if_not_marked_as_present() {
    JsonArrayTest msg = JsonArrayTest.newBuilder()
      .addANullableArray("foo")
      .build();

    // repeated field "aNullableArray" is present, but the boolean marker "aNullableArrayPresentIfEmpty"
    // is not set.
    assertThat(msg.hasANullableArrayPresentIfEmpty()).isFalse();
    assertThat(msg.getANullableArrayPresentIfEmpty()).isFalse();

    // JSON contains the array but not the boolean marker
    assertThat(toJson(msg)).isEqualTo("{\"aNullableArray\":[\"foo\"]}");
  }
}
