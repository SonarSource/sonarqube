/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.protobuf.Descriptors;
import com.google.protobuf.MapEntry;
import com.google.protobuf.Message;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.sonar.api.utils.text.JsonWriter;

/**
 * Converts a Protocol Buffers message to JSON. Unknown fields, binary fields and groups
 * are not supported. Absent fields are ignored, so it's possible to distinguish
 * null strings (field is absent) and empty strings (field is present with value {@code ""}).
 *
 * <h3>Example</h3>
 * <pre>
 *   // protobuf specification
 *   message Foo {
 *     string name = 1;
 *     int32 count = 2;
 *     repeated string colors = 3;
 *   }
 *
 *   // generated JSON
 *   {
 *     "name": "hello",
 *     "count": 32,
 *     "colors": ["blue", "red"]
 *   }
 * </pre>
 *
 * <h3>Absent versus empty arrays</h3>
 * <p>
 * Protobuf does not support absent repeated field. The default value is empty. A pattern
 * is defined by {@link ProtobufJsonFormat} to support the difference between absent and
 * empty arrays when generating JSON. An intermediary message wrapping the array must be defined.
 * It is automatically inlined and does not appear in the generated JSON. This wrapper message must:
 * </p>
 * <ul>
 *   <li>contain a single repeated field</li>
 *   <li>has the same name (case-insensitive) as the field</li>
 * </ul>
 *
 *
 * <p>Example:</p>
 * <pre>
 *   // protobuf specification
 *   message Continent {
 *     string name = 1;
 *     Countries countries = 2;
 *   }
 *
 *   // the message name ("Countries") is the same as the field 1 ("countries")
 *   message Countries {
 *     repeated string countries = 1;
 *   }
 *
 *   // example of generated JSON if field 2 is not present
 *   {
 *     "name": "Europe",
 *   }
 *
 *   // example of generated JSON if field 2 is present but inner array is empty.
 *   // The intermediary "countries" message is inline.
 *   {
 *     "name": "Europe",
 *     "countries": []
 *   }
 *
 *   // example of generated JSON if field 2 is present and inner array is not empty
 *   // The intermediary "countries" message is inline.
 *   {
 *     "name": "Europe",
 *     "countries": ["Spain", "Denmark"]
 *   }
 * </pre>
 *
 * <h3>Array or map in map values</h3>
 * <p>
 *   Map fields cannot be repeated. Values are scalar types or messages, but not arrays nor maps. In order
 * to support multimaps (maps of lists) and maps of maps in JSON, the same pattern as for absent arrays
 * can be used:
 * </p>
 * <p>Example:</p>
 * <pre>
 *   // protobuf specification
 *   message Continent {
 *     string name = 1;
 *     map&lt;string,Countries&gt; countries_by_currency = 2;
 *   }
 *
 *   // the message name ("Countries") is the same as the field 1 ("countries")
 *   message Countries {
 *     repeated string countries = 1;
 *   }
 *
 *   // example of generated JSON. The intermediary "countries" message is inline.
 *   {
 *     "name": "Europe",
 *     "countries_by_currency": {
 *       "eur": ["Spain", "France"],
 *       "dkk": ["Denmark"]
 *     }
 *   }
 * </pre>
 */
public class ProtobufJsonFormat {

  private ProtobufJsonFormat() {
    // only statics
  }

  static class MessageType {
    private static final Map<Class<? extends Message>, MessageType> TYPES_BY_CLASS = new HashMap<>();

    private final Descriptors.FieldDescriptor[] fieldDescriptors;
    private final boolean doesWrapRepeated;

    private MessageType(Descriptors.Descriptor descriptor) {
      this.fieldDescriptors = descriptor.getFields().toArray(new Descriptors.FieldDescriptor[descriptor.getFields().size()]);
      this.doesWrapRepeated = fieldDescriptors.length == 1 && fieldDescriptors[0].isRepeated() && descriptor.getName().equalsIgnoreCase(fieldDescriptors[0].getName());
    }

    static MessageType of(Message message) {
      MessageType type = TYPES_BY_CLASS.get(message.getClass());
      if (type == null) {
        type = new MessageType(message.getDescriptorForType());
        TYPES_BY_CLASS.put(message.getClass(), type);
      }
      return type;
    }
  }

  public static void write(Message message, JsonWriter writer) {
    writer.setSerializeNulls(false).setSerializeEmptys(true);
    writer.beginObject();
    writeMessage(message, writer);
    writer.endObject();
  }

  public static String toJson(Message message) {
    StringWriter json = new StringWriter();
    try (JsonWriter jsonWriter = JsonWriter.of(json)) {
      write(message, jsonWriter);
    }
    return json.toString();
  }

  private static void writeMessage(Message message, JsonWriter writer) {
    MessageType type = MessageType.of(message);
    for (Descriptors.FieldDescriptor fieldDescriptor : type.fieldDescriptors) {
      if (fieldDescriptor.isRepeated()) {
        writer.name(fieldDescriptor.getName());
        if (fieldDescriptor.isMapField()) {
          writeMap((Collection<MapEntry>) message.getField(fieldDescriptor), writer);
        } else {
          writeArray(writer, fieldDescriptor, (Collection) message.getField(fieldDescriptor));
        }
      } else if (message.hasField(fieldDescriptor)) {
        writer.name(fieldDescriptor.getName());
        Object fieldValue = message.getField(fieldDescriptor);
        writeFieldValue(fieldDescriptor, fieldValue, writer);
      }
    }
  }

  private static void writeArray(JsonWriter writer, Descriptors.FieldDescriptor fieldDescriptor, Collection array) {
    writer.beginArray();
    for (Object o : array) {
      writeFieldValue(fieldDescriptor, o, writer);
    }
    writer.endArray();
  }

  private static void writeMap(Collection<MapEntry> mapEntries, JsonWriter writer) {
    writer.beginObject();
    for (MapEntry mapEntry : mapEntries) {
      // Key fields are always double-quoted in json
      writer.name(mapEntry.getKey().toString());
      Descriptors.FieldDescriptor valueDescriptor = mapEntry.getDescriptorForType().findFieldByName("value");
      writeFieldValue(valueDescriptor, mapEntry.getValue(), writer);
    }
    writer.endObject();
  }

  private static void writeFieldValue(Descriptors.FieldDescriptor fieldDescriptor, Object value, JsonWriter writer) {
    switch (fieldDescriptor.getJavaType()) {
      case INT:
        writer.value((Integer) value);
        break;
      case LONG:
        writer.value((Long) value);
        break;
      case DOUBLE:
        writer.value((Double) value);
        break;
      case BOOLEAN:
        writer.value((Boolean) value);
        break;
      case STRING:
        writer.value((String) value);
        break;
      case ENUM:
        writer.value(((Descriptors.EnumValueDescriptor) value).getName());
        break;
      case MESSAGE:
        writeMessageValue((Message) value, writer);
        break;
      default:
        throw new IllegalStateException(String.format("JSON format does not support type '%s' of field '%s'", fieldDescriptor.getJavaType(), fieldDescriptor.getName()));
    }
  }

  private static void writeMessageValue(Message message, JsonWriter writer) {
    MessageType messageType = MessageType.of(message);
    if (messageType.doesWrapRepeated) {
      Descriptors.FieldDescriptor repeatedDescriptor = messageType.fieldDescriptors[0];
      if (repeatedDescriptor.isMapField()) {
        writeMap((Collection<MapEntry>) message.getField(repeatedDescriptor), writer);
      } else {
        writeArray(writer, repeatedDescriptor, (Collection) message.getField(repeatedDescriptor));
      }
    } else {
      writer.beginObject();
      writeMessage(message, writer);
      writer.endObject();
    }
  }
}
