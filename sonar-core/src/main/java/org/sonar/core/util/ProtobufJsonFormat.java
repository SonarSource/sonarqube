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

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import java.util.Collection;
import java.util.Map;
import org.sonar.api.utils.text.JsonWriter;

/**
 * Converts a Protocol Buffers message to JSON. Unknown fields, binary fields, (deprecated) groups
 * and maps are not supported.
 */
public class ProtobufJsonFormat {

  private ProtobufJsonFormat() {
    // only statics
  }

  public static void write(Message message, JsonWriter writer) {
    writer.beginObject();
    writeMessage(message, writer);
    writer.endObject();
  }

  private static void writeMessage(Message message, JsonWriter writer) {
    for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : message.getAllFields().entrySet()) {
      writeField(entry.getKey(), entry.getValue(), writer);
    }
  }

  private static void writeField(Descriptors.FieldDescriptor field, Object value, JsonWriter writer) {
    writer.name(field.getName());
    if (field.isRepeated()) {
      // Repeated field. Print each element.
      writer.beginArray();
      for (Object o : (Collection) value) {
        writeFieldValue(field, o, writer);
      }
      writer.endArray();
    } else {
      writeFieldValue(field, value, writer);
    }
  }

  private static void writeFieldValue(Descriptors.FieldDescriptor field, Object value, JsonWriter writer) {
    switch (field.getJavaType()) {
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
      case BYTE_STRING:
        throw new IllegalStateException(String.format("JSON format does not support the binary field '%s'", field.getName()));
      case ENUM:
        writer.value(((Descriptors.EnumValueDescriptor) value).getName());
        break;
      case MESSAGE:
        writer.beginObject();
        writeMessage((Message) value, writer);
        writer.endObject();
        break;
      default:
    }
  }
}
