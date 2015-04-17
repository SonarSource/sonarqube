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
import java.util.Map;

/**
 * Writes JSON as a stream. This class allows plugins to not directly depend
 * on the underlying JSON library.
 * <p>
 * <h3>How to use</h3>
 * <pre>
 *   StringWriter json = new StringWriter();
 *   JsonWriter writer = JsonWriter.of(json);
 *   writer
 *     .beginObject()
 *     .prop("aBoolean", true)
 *     .prop("aInt", 123)
 *     .prop("aString", "foo")
 *     .beginObject().name("aList")
 *       .beginArray()
 *         .beginObject().prop("key", "ABC").endObject()
 *         .beginObject().prop("key", "DEF").endObject()
 *       .endArray()
 *     .endObject()
 *     .close();
 * </pre>
 * </p>
 * <p>
 * By default, null objects are not serialized. To enable {@code null} serialization,
 * use {@link #setSerializeNulls(boolean)}.
 * </p>
 * <p>
 * By default, emptry strings are serialized. To disable empty string serialization,
 * use {@link #setSerializeEmptys(boolean)}.
 * </p>
 *
 * @since 4.2
 */
public class JsonWriter {

  private final com.google.gson.stream.JsonWriter stream;
  private boolean serializeEmptyStrings;

  private JsonWriter(Writer writer) {
    this.stream = new com.google.gson.stream.JsonWriter(writer);
    this.stream.setSerializeNulls(false);
    this.stream.setLenient(false);
    this.serializeEmptyStrings = true;
  }

  // for unit testing
  JsonWriter(com.google.gson.stream.JsonWriter stream) {
    this.stream = stream;
  }

  public static JsonWriter of(Writer writer) {
    return new JsonWriter(writer);
  }

  public JsonWriter setSerializeNulls(boolean b) {
    this.stream.setSerializeNulls(b);
    return this;
  }

  /**
   * Enable/disable serialization of properties which value is an empty String.
   */
  public JsonWriter setSerializeEmptys(boolean serializeEmptyStrings) {
    this.serializeEmptyStrings = serializeEmptyStrings;
    return this;
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
   *
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
   *
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
   *
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
   *
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
   *
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
      stream.value(serializeEmptyStrings ? value : emptyToNull(value));
      return this;
    } catch (Exception e) {
      throw rethrow(e);
    }
  }

  /**
   * Encodes an object that can be a :
   * <ul>
   * <li>primitive types: String, Number, Boolean</li>
   * <li>java.util.Date: encoded as datetime (see {@link #valueDateTime(java.util.Date)}</li>
   * <li><code>Map<Object, Object></code>. Method toString is called for the key.</li>
   * <li>Iterable</li>
   * </ul>
   *
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public JsonWriter valueObject(@Nullable Object value) {
    try {
      if (value == null) {
        stream.nullValue();
      } else {
        if (value instanceof String) {
          stream.value(serializeEmptyStrings ? (String) value : emptyToNull((String) value));
        } else if (value instanceof Number) {
          stream.value((Number) value);
        } else if (value instanceof Boolean) {
          stream.value((Boolean) value);
        } else if (value instanceof Date) {
          valueDateTime((Date) value);
        } else if (value instanceof Enum) {
          stream.value(((Enum) value).name());
        } else if (value instanceof Map) {
          stream.beginObject();
          for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) value).entrySet()) {
            stream.name(entry.getKey().toString());
            valueObject(entry.getValue());
          }
          stream.endObject();
        } else if (value instanceof Iterable) {
          stream.beginArray();
          for (Object o : (Iterable<Object>) value) {
            valueObject(o);
          }
          stream.endArray();
        } else {
          throw new IllegalArgumentException(getClass() + " does not support encoding of type: " + value.getClass());
        }
      }
      return this;
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw rethrow(e);
    }
  }

  /**
   * Write a list of values in an array, for example:
   * <pre>
   *   writer.beginArray().values(myValues).endArray();
   * </pre>
   *
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public JsonWriter values(Iterable<String> values) {
    for (String value : values) {
      value(value);
    }
    return this;
  }

  /**
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public JsonWriter valueDate(@Nullable Date value) {
    try {
      stream.value(value == null ? null : DateUtils.formatDate(value));
      return this;
    } catch (Exception e) {
      throw rethrow(e);
    }
  }

  public JsonWriter valueDateTime(@Nullable Date value) {
    try {
      stream.value(value == null ? null : DateUtils.formatDateTime(value));
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
   *
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public JsonWriter prop(String name, @Nullable Number value) {
    return name(name).value(value);
  }

  /**
   * Encodes the property name and date value (ISO format).
   * Output is for example <code>"theDate":"2013-01-24"</code>.
   *
   * @throws org.sonar.api.utils.text.WriterException on any failure
   */
  public JsonWriter propDate(String name, @Nullable Date value) {
    return name(name).valueDate(value);
  }

  /**
   * Encodes the property name and datetime value (ISO format).
   * Output is for example <code>"theDate":"2013-01-24T13:12:45+01"</code>.
   *
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

  @Nullable
  private static String emptyToNull(@Nullable String value) {
    if (value == null || value.isEmpty()) {
      return null;
    }
    return value;
  }
}
