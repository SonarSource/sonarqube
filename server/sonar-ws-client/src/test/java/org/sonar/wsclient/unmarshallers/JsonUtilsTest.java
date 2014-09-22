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
package org.sonar.wsclient.unmarshallers;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Test;

import java.util.Date;
import java.util.TimeZone;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class JsonUtilsTest extends UnmarshallerTestCase {

  @Test
  public void getIntFields() {
    JSONObject obj = (JSONObject) JSONValue.parse("{\"one\": 1, \"two\": 2}");
    assertThat(JsonUtils.getInteger(obj, "one"), is(1));
    assertThat(JsonUtils.getInteger(obj, "two"), is(2));
    assertThat(JsonUtils.getInteger(obj, "three"), nullValue());
  }

  @Test
  public void getLongFields() {
    JSONObject obj = (JSONObject) JSONValue.parse("{\"one\": 1, \"two\": 2}");
    assertThat(JsonUtils.getLong(obj, "one"), is(1l));
    assertThat(JsonUtils.getLong(obj, "two"), is(2l));
    assertThat(JsonUtils.getLong(obj, "three"), nullValue());
  }

  @Test
  public void getStringFields() {
    JSONObject obj = (JSONObject) JSONValue.parse("{\"one\": \"1\", \"two\": \"2\"}");
    assertThat(JsonUtils.getString(obj, "one"), is("1"));
    assertThat(JsonUtils.getString(obj, "two"), is("2"));
    assertThat(JsonUtils.getString(obj, "three"), nullValue());
  }

  @Test
  public void getNumberAsString() {
    JSONObject obj = (JSONObject) JSONValue.parse("{\"one\": 1, \"two\": 2}");
    assertThat(JsonUtils.getString(obj, "one"), is("1"));
    assertThat(JsonUtils.getString(obj, "two"), is("2"));
  }

  @Test
  public void getDateField() {
    JSONObject obj = (JSONObject) JSONValue.parse("{\"foo\": \"2009-12-25\", \"two\": \"2\"}");
    Date date = JsonUtils.getDate(obj, "foo");
    assertThat(date.getDate(), is(25));
    assertThat(date.getMonth(), is(11));
    assertThat(date.getYear(), is(109));
  }

  @Test
  public void getDateTimeField() {
    TimeZone defaultTimeZone = TimeZone.getDefault();
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
      JSONObject obj = (JSONObject) JSONValue.parse("{\"foo\": \"2009-12-25T15:59:59-0100\", \"two\": \"2\"}");
      Date date = JsonUtils.getDateTime(obj, "foo");
      assertThat(date.getDate(), is(25));
      assertThat(date.getMonth(), is(11));
      assertThat(date.getYear(), is(109));
      assertThat(date.getHours(), is(16));
      assertThat(date.getMinutes(), is(59));
    } finally {
      TimeZone.setDefault(defaultTimeZone);
    }
  }
}
