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
package org.sonar.api.utils.text;

import org.sonar.api.utils.DateUtils;

import javax.annotation.Nullable;
import java.io.Writer;
import java.util.Date;

/**
 * @since 4.2
 */
public class JsonWriter {

  public static JsonWriter of(Writer writer) {
    return new JsonWriter(writer);
  }

  private final com.google.gson.stream.JsonWriter stream;

  private JsonWriter(Writer writer) {
    this.stream = new com.google.gson.stream.JsonWriter(writer);
    this.stream.setSerializeNulls(false);
    this.stream.setLenient(false);
  }

  // for unit testing
  JsonWriter(com.google.gson.stream.JsonWriter stream) {
    this.stream = stream;
  }

  /**
   * Begins encoding a new array. Each call to this method must be paired with
   * a call to {@link #endArray}. Output is <code>[</code>.
   *
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public JsonWriter beginArray() {
    try {
      stream.beginArray();
      return this;
    } catch (Exception e) {
      throw rethrow(e);
    }
  }

  /**
   * Ends encoding the current array. Output is <code>]</code>.
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public JsonWriter endArray() {
    try {
      stream.endArray();
      return this;
    } catch (Exception e) {
      throw rethrow(e);
    }
  }

  /**
   * Begins encoding a new object. Each call to this method must be paired
   * with a call to {@link #endObject}. Output is <code>{</code>.
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public JsonWriter beginObject() {
    try {
      stream.beginObject();
      return this;
    } catch (Exception e) {
      throw rethrow(e);
    }
  }

  /**
   * Ends encoding the current object. Output is <code>}</code>.
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public JsonWriter endObject() {
    try {
      stream.endObject();
      return this;
    } catch (Exception e) {
      throw rethrow(e);
    }
  }

  /**
   * Encodes the property name. Output is <code>"theName":</code>.
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public JsonWriter name(String name) {
    try {
      stream.name(name);
      return this;
    } catch (Exception e) {
      throw rethrow(e);
    }
  }

  /**
   * Encodes {@code value}. Output is <code>true</code> or <code>false</code>.
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public JsonWriter value(boolean value) {
    try {
      stream.value(value);
      return this;
    } catch (Exception e) {
      throw rethrow(e);
    }
  }

  /**
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public JsonWriter value(double value) {
    try {
      stream.value(value);
      return this;
    } catch (Exception e) {
      throw rethrow(e);
    }
  }

  /**
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public JsonWriter value(@Nullable String value) {
    try {
      stream.value(value);
      return this;
    } catch (Exception e) {
      throw rethrow(e);
    }
  }

  /**
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public JsonWriter valueDate(@Nullable Date value) {
    try {
      stream.value(value==null ? null : DateUtils.formatDate(value));
      return this;
    } catch (Exception e) {
      throw rethrow(e);
    }
  }

  public JsonWriter valueDateTime(@Nullable Date value) {
    try {
      stream.value(value==null ? null : DateUtils.formatDateTime(value));
      return this;
    } catch (Exception e) {
      throw rethrow(e);
    }
  }

  /**
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public JsonWriter value(long value) {
    try {
      stream.value(value);
      return this;
    } catch (Exception e) {
      throw rethrow(e);
    }
  }

  /**
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public JsonWriter value(@Nullable Number value) {
    try {
      stream.value(value);
      return this;
    } catch (Exception e) {
      throw rethrow(e);
    }
  }

  /**
   * Encodes the property name and value. Output is for example <code>"theName":123</code>.
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public JsonWriter prop(String name, @Nullable Number value) {
    return name(name).value(value);
  }

  /**
   * Encodes the property name and date value (ISO format).
   * Output is for example <code>"theDate":"2013-01-24"</code>.
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public JsonWriter propDate(String name, @Nullable Date value) {
    return name(name).valueDate(value);
  }

  /**
   * Encodes the property name and datetime value (ISO format).
   * Output is for example <code>"theDate":"2013-01-24T13:12:45+01"</code>.
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public JsonWriter propDateTime(String name, @Nullable Date value) {
    return name(name).valueDateTime(value);
  }

  /**
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public JsonWriter prop(String name, @Nullable String value) {
    return name(name).value(value);
  }

  /**
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public JsonWriter prop(String name, boolean value) {
    return name(name).value(value);
  }

  /**
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public JsonWriter prop(String name, long value) {
    return name(name).value(value);
  }

  /**
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public JsonWriter prop(String name, double value) {
    return name(name).value(value);
  }

  /**
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public void close() {
    try {
      stream.close();
    } catch (Exception e) {
      throw rethrow(e);
    }
  }

  private IllegalStateException rethrow(Exception e) {
    // stacktrace is not helpful
    throw new WriterException("Fail to write JSON: " + e.getMessage());
  }
}
