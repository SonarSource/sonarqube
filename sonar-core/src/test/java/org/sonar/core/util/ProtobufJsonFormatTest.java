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
import org.sonar.core.test.Test.ArrayFieldMsg;
import org.sonar.core.test.Test.MapMsg;
import org.sonar.core.test.Test.PrimitiveTypeMsg;
import org.sonar.test.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.util.ProtobufJsonFormat.toJson;

public class ProtobufJsonFormatTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void test_conversion_of_primitive_types() {
    PrimitiveTypeMsg protobuf = PrimitiveTypeMsg.newBuilder()
      .setStringField("foo")
      .setIntField(10)
      .setLongField(100L)
      .setDoubleField(3.14)
      .setBooleanField(true)
      .setEnumField(org.sonar.core.test.Test.FakeEnum.GREEN)
      .build();

    assertThat(toJson(protobuf)).isEqualTo(
      "{\"stringField\":\"foo\",\"intField\":10,\"longField\":100,\"doubleField\":3.14,\"booleanField\":true,\"enumField\":\"GREEN\"}");
  }

  @Test
  public void bytes_field_can_not_be_converted() {
    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("JSON format does not support type 'BYTE_STRING' of field 'bytesField'");

    PrimitiveTypeMsg protobuf = PrimitiveTypeMsg.newBuilder()
      .setBytesField(ByteString.copyFrom(new byte[] {2, 4}))
      .build();

    ProtobufJsonFormat.write(protobuf, JsonWriter.of(new StringWriter()));
  }

  @Test
  public void do_not_convert_absent_primitive_fields() {
    PrimitiveTypeMsg msg = PrimitiveTypeMsg.newBuilder().build();

    // fields are absent
    assertThat(msg.hasStringField()).isFalse();
    assertThat(msg.hasIntField()).isFalse();
  }

  @Test
  public void convert_empty_string() {
    PrimitiveTypeMsg msg = PrimitiveTypeMsg.newBuilder().setStringField("").build();

    // field is present
    assertThat(msg.hasStringField()).isTrue();
    assertThat(msg.getStringField()).isEqualTo("");

    assertThat(toJson(msg)).isEqualTo("{\"stringField\":\"\"}");
  }

  @Test
  public void convert_arrays() {
    ArrayFieldMsg msg = ArrayFieldMsg.newBuilder()
      .addStrings("one").addStrings("two")
      .addNesteds(org.sonar.core.test.Test.NestedMsg.newBuilder().setLabel("nestedOne")).addNesteds(org.sonar.core.test.Test.NestedMsg.newBuilder().setLabel("nestedTwo"))
      .addNullableArray("nullableOne").addNullableArray("nullableTwo")
      .build();

    assertThat(toJson(msg))
      .isEqualTo("{\"strings\":[\"one\",\"two\"],\"nesteds\":[{\"label\":\"nestedOne\"},{\"label\":\"nestedTwo\"}],\"nullableArray\":[\"nullableOne\",\"nullableTwo\"]}");
  }

  @Test
  public void convert_empty_arrays() {
    ArrayFieldMsg msg = ArrayFieldMsg.newBuilder()
      .setNullableArrayPresentIfEmpty(true)
      .build();

    assertThat(toJson(msg)).isEqualTo("{\"strings\":[],\"nesteds\":[],\"nullableArray\":[]}");
  }

  @Test
  public void do_not_convert_empty_array_marked_as_absent() {
    ArrayFieldMsg msg = ArrayFieldMsg.newBuilder()
      .setNullableArrayPresentIfEmpty(false)
      .build();

    assertThat(msg.hasNullableArrayPresentIfEmpty()).isTrue();
    assertThat(msg.getNullableArrayPresentIfEmpty()).isFalse();

    // nullableArray does not appear
    assertThat(toJson(msg)).isEqualTo("{\"strings\":[],\"nesteds\":[]}");
  }

  @Test
  public void convert_non_empty_array_even_if_not_marked_as_present() {
    ArrayFieldMsg msg = ArrayFieldMsg.newBuilder()
      .addNullableArray("foo")
      .build();

    // repeated field "nullableArray" is present, but the boolean marker "nullableArrayPresentIfEmpty"
    // is not set.
    assertThat(msg.hasNullableArrayPresentIfEmpty()).isFalse();
    assertThat(msg.getNullableArrayPresentIfEmpty()).isFalse();

    // JSON contains the array but not the boolean marker
    assertThat(toJson(msg)).contains("\"nullableArray\":[\"foo\"]");
  }

  @Test
  public void convert_map() {
    MapMsg.Builder builder = MapMsg.newBuilder();
    builder.getMutableStringMap().put("one", "un");
    builder.getMutableStringMap().put("two", "deux");
    builder.getMutableNestedMap().put("three", org.sonar.core.test.Test.NestedMsg.newBuilder().setLabel("trois").build());
    builder.getMutableNestedMap().put("four", org.sonar.core.test.Test.NestedMsg.newBuilder().setLabel("quatre").build());
    builder.setNullableStringMapPresentIfEmpty(false);
    assertThat(toJson(builder.build()))
      .isEqualTo("{\"stringMap\":{\"one\":\"un\",\"two\":\"deux\"},\"nestedMap\":{\"three\":{\"label\":\"trois\"},\"four\":{\"label\":\"quatre\"}}}");
  }

  @Test
  public void convert_empty_map() {
    MapMsg msg = MapMsg.newBuilder().build();
    assertThat(toJson(msg)).isEqualTo("{\"stringMap\":{},\"nestedMap\":{}}");
  }

  @Test
  public void convert_nullable_empty_map_if_marked_as_present() {
    MapMsg msg = MapMsg.newBuilder().setNullableStringMapPresentIfEmpty(true).build();
    assertThat(toJson(msg)).isEqualTo("{\"stringMap\":{},\"nestedMap\":{},\"nullableStringMap\":{}}");
  }

  @Test
  public void constructor_is_private() throws Exception {
    assertThat(TestUtils.hasOnlyPrivateConstructors(ProtobufJsonFormat.class)).isTrue();
  }
}
