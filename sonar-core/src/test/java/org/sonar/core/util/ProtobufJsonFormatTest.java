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

import com.google.protobuf.ByteString;
import java.io.StringWriter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.test.Test.Countries;
import org.sonar.core.test.Test.Country;
import org.sonar.core.test.Test.NestedMsg;
import org.sonar.core.test.Test.PrimitiveTypeMsg;
import org.sonar.core.test.Test.TestArray;
import org.sonar.core.test.Test.TestMap;
import org.sonar.core.test.Test.TestMapOfArray;
import org.sonar.core.test.Test.TestMapOfMap;
import org.sonar.core.test.Test.TestNullableArray;
import org.sonar.core.test.Test.TestNullableMap;
import org.sonar.core.test.Test.Translations;
import org.sonar.test.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.util.ProtobufJsonFormat.toJson;

public class ProtobufJsonFormatTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void test_primitive_types() {
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
      .setBytesField(ByteString.copyFrom(new byte[]{2, 4}))
      .build();

    ProtobufJsonFormat.write(protobuf, JsonWriter.of(new StringWriter()));
  }

  @Test
  public void do_not_write_null_primitive_fields() {
    PrimitiveTypeMsg msg = PrimitiveTypeMsg.newBuilder().build();

    // fields are absent
    assertThat(msg.hasStringField()).isFalse();
    assertThat(msg.hasIntField()).isFalse();
  }

  @Test
  public void write_empty_string() {
    PrimitiveTypeMsg msg = PrimitiveTypeMsg.newBuilder().setStringField("").build();

    // field is present
    assertThat(msg.hasStringField()).isTrue();
    assertThat(msg.getStringField()).isEqualTo("");

    assertThat(toJson(msg)).isEqualTo("{\"stringField\":\"\"}");
  }

  @Test
  public void write_array() {
    TestArray msg = TestArray.newBuilder()
      .addStrings("one").addStrings("two")
      .addNesteds(NestedMsg.newBuilder().setLabel("nestedOne")).addNesteds(NestedMsg.newBuilder().setLabel("nestedTwo"))
      .build();

    assertThat(toJson(msg))
      .isEqualTo("{\"strings\":[\"one\",\"two\"],\"nesteds\":[{\"label\":\"nestedOne\"},{\"label\":\"nestedTwo\"}]}");
  }

  @Test
  public void write_empty_array() {
    TestArray msg = TestArray.newBuilder().build();

    assertThat(toJson(msg)).isEqualTo("{\"strings\":[],\"nesteds\":[]}");
  }

  @Test
  public void do_not_write_null_wrapper_of_array() {
    TestNullableArray msg = TestNullableArray.newBuilder()
      .setLabel("world")
      .build();

    assertThat(msg.hasCountries()).isFalse();

    // array wrapper is null
    assertThat(toJson(msg)).isEqualTo("{\"label\":\"world\"}");
  }

  @Test
  public void inline_wrapper_of_array() {
    TestNullableArray msg = TestNullableArray.newBuilder()
      .setLabel("world")
      .setCountries(Countries.newBuilder())
      .build();
    assertThat(msg.hasCountries()).isTrue();
    assertThat(toJson(msg)).contains("\"label\":\"world\",\"countries\":[]");

    msg = TestNullableArray.newBuilder()
      .setLabel("world")
      .setCountries(Countries.newBuilder().addCountries(Country.newBuilder().setName("France").setContinent("Europe")))
      .build();
    assertThat(msg.hasCountries()).isTrue();
    assertThat(toJson(msg)).contains("\"label\":\"world\",\"countries\":[{\"name\":\"France\",\"continent\":\"Europe\"}]");
  }

  @Test
  public void write_map() {
    TestMap.Builder builder = TestMap.newBuilder();
    builder.getMutableStringMap().put("one", "un");
    builder.getMutableStringMap().put("two", "deux");
    builder.getMutableNestedMap().put("three", NestedMsg.newBuilder().setLabel("trois").build());
    builder.getMutableNestedMap().put("four", NestedMsg.newBuilder().setLabel("quatre").build());
    assertThat(toJson(builder.build())).isEqualTo(
      "{\"stringMap\":{\"one\":\"un\",\"two\":\"deux\"},\"nestedMap\":{\"three\":{\"label\":\"trois\"},\"four\":{\"label\":\"quatre\"}}}");
  }

  @Test
  public void write_empty_map() {
    TestMap.Builder builder = TestMap.newBuilder();
    assertThat(toJson(builder.build())).isEqualTo("{\"stringMap\":{},\"nestedMap\":{}}");
  }

  @Test
  public void do_not_write_null_wrapper_of_map() {
    TestNullableMap msg = TestNullableMap.newBuilder()
      .setLabel("world")
      .build();
    assertThat(toJson(msg)).isEqualTo("{\"label\":\"world\"}");
  }

  @Test
  public void inline_wrapper_of_map() {
    TestNullableMap msg = TestNullableMap.newBuilder()
      .setLabel("world")
      .setTranslations(Translations.newBuilder())
      .build();
    assertThat(toJson(msg)).isEqualTo("{\"label\":\"world\",\"translations\":{}}");

    Translations.Builder translationsBuilder = Translations.newBuilder();
    translationsBuilder.getMutableTranslations().put("one", "un");
    translationsBuilder.getMutableTranslations().put("two", "deux");
    msg = TestNullableMap.newBuilder()
      .setLabel("world")
      .setTranslations(translationsBuilder)
      .build();
    assertThat(toJson(msg)).isEqualTo("{\"label\":\"world\",\"translations\":{\"one\":\"un\",\"two\":\"deux\"}}");
  }

  @Test
  public void write_map_of_arrays() throws Exception {
    // this is a trick to have arrays in map values
    TestMapOfArray.Builder msg = TestMapOfArray.newBuilder();

    // wrapper over array
    Countries europe = Countries.newBuilder()
      .addCountries(Country.newBuilder().setContinent("Europe").setName("France"))
      .addCountries(Country.newBuilder().setContinent("Europe").setName("Germany"))
      .build();
    msg.getMutableMoneys().put("eur", europe);
    assertThat(toJson(msg.build())).isEqualTo("{\"moneys\":{\"eur\":[{\"name\":\"France\",\"continent\":\"Europe\"},{\"name\":\"Germany\",\"continent\":\"Europe\"}]}}");
  }

  @Test
  public void write_map_of_map() throws Exception {
    // this is a trick to have maps in map values
    TestMapOfMap.Builder msg = TestMapOfMap.newBuilder();

    // wrapper over map
    Translations.Builder translationsBuilder = Translations.newBuilder();
    translationsBuilder.getMutableTranslations().put("one", "un");
    translationsBuilder.getMutableTranslations().put("two", "deux");
    msg.getMutableCatalogs().put("numbers", translationsBuilder.build());
    assertThat(toJson(msg.build())).isEqualTo("{\"catalogs\":{\"numbers\":{\"one\":\"un\",\"two\":\"deux\"}}}");
  }

  @Test
  public void constructor_is_private() {
    assertThat(TestUtils.hasOnlyPrivateConstructors(ProtobufJsonFormat.class)).isTrue();
  }
}
