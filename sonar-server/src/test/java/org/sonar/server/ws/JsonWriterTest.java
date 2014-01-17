/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.StringWriter;

import static org.fest.assertions.Assertions.assertThat;

public class JsonWriterTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  StringWriter json = new StringWriter();
  JsonWriter writer = JsonWriter.of(json);

  private void expect(String s) {
    assertThat(json.toString()).isEqualTo(s);
  }

  @Test
  public void empty_object() throws Exception {
    writer.beginObject().endObject().close();
    expect("{}");
  }

  @Test
  public void empty_array() throws Exception {
    writer.beginArray().endArray().close();
    expect("[]");
  }

  @Test
  public void stop_while_streaming() throws Exception {
    writer.beginObject().name("foo").value("bar");
    // endObject() and close() are missing
    expect("{\"foo\":\"bar\"");
  }

  @Test
  public void objects_and_arrays() throws Exception {
    writer.beginObject().name("issues")
      .beginArray()
      .beginObject().prop("key", "ABC").endObject()
      .beginObject().prop("key", "DEF").endObject()
      .endArray()
      .endObject().close();
    expect("{\"issues\":[{\"key\":\"ABC\"},{\"key\":\"DEF\"}]}");
  }

  @Test
  public void ignore_null_values() throws Exception {
    writer.beginObject()
      .prop("nullNumber", (Number) null)
      .prop("nullString", (String) null)
      .name("nullNumber").value((Number) null)
      .name("nullString").value((String) null)
      .endObject().close();
    expect("{}");
  }

  @Test
  public void escape_values() throws Exception {
    writer.beginObject()
      .prop("foo", "<hello \"world\">")
      .endObject().close();
    expect("{\"foo\":\"<hello \\\"world\\\">\"}");

  }

  @Test
  public void fail_on_NaN_value() throws Exception {
    thrown.expect(WriterException.class);
    writer.beginObject().prop("foo", Double.NaN).endObject().close();
  }

  @Test
  public void fail_if_not_valid() throws Exception {
    thrown.expect(WriterException.class);
    writer.beginObject().endArray().close();
  }
}
