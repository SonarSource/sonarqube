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

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.api.utils.text.JsonWriter;

/**
 * Converts a Protocol Buffers message to JSON. Unknown fields, binary fields, (deprecated) groups
 * and maps are not supported. Absent fields are ignored, so it's possible to distinguish
 * null strings (field is absent) and empty strings (field is present with value {@code ""}).
 * <p/>
 * <h2>Empty Arrays</h2>
 * Protobuf does not make the difference between absent arrays and empty arrays (size is zero).
 * The consequence is that arrays are always output in JSON. Empty arrays are converted to {@code []}.
 * <p/>
 * A workaround is implemented in {@link ProtobufJsonFormat} to not generate absent arrays into JSON document.
 * A boolean field is used to declare if the related repeated field (the array) is present or not. The
 * name of the boolean field must be the array field name suffixed with "PresentIfEmpty". This field is "for internal
 * use" and is not generated into JSON document. It is ignored when the array is not empty.
 *
 * For example:
 * <pre>
 *   // proto specification
 *   message Response {
 *     optional bool issuesPresentIfEmpty = 1;
 *     repeated Issue issues = 2;
 *   }
 * </pre>
 * <pre>
 *   // Java usage
 *
 *   Response.newBuilder().build();
 *   // output: {}
 *
 *   Response.newBuilder().setIssuesPresentIfEmpty(true).build();
 *   // output: {"issues": []}
 *
 *   // no need to set the flag to true when the array is not empty
 *   Response.newBuilder().setIssues(atLeastOneIssues).build();
 *   // output: {"issues": [{...}, {...}]}
 * </pre>
 */
public class ProtobufJsonFormat {

  private ProtobufJsonFormat() {
    // only statics
  }

  private static abstract class MessageField {
    protected final Descriptors.FieldDescriptor descriptor;

    public MessageField(Descriptors.FieldDescriptor descriptor) {
      this.descriptor = descriptor;
    }

    public String getName() {
      return descriptor.getName();
    }

    public Descriptors.FieldDescriptor.JavaType getJavaType() {
      return descriptor.getJavaType();
    }

    public abstract boolean isRepeated();

    public abstract boolean hasValue(Message message);

    public abstract Object getValue(Message message);
  }

  private static class MessageNonRepeatedField extends MessageField {
    public MessageNonRepeatedField(Descriptors.FieldDescriptor descriptor) {
      super(descriptor);
      Preconditions.checkArgument(!descriptor.isRepeated());
    }

    @Override
    public boolean isRepeated() {
      return false;
    }

    @Override
    public boolean hasValue(Message message) {
      return message.hasField(descriptor);
    }

    @Override
    public Object getValue(Message message) {
      return message.getField(descriptor);
    }
  }

  private static class MessageRepeatedField extends MessageField {
    public MessageRepeatedField(Descriptors.FieldDescriptor descriptor) {
      super(descriptor);
      Preconditions.checkArgument(descriptor.isRepeated());
    }

    @Override
    public boolean isRepeated() {
      return true;
    }

    @Override
    public boolean hasValue(Message message) {
      return true;
    }

    @Override
    public Object getValue(Message message) {
      return message.getField(descriptor);
    }
  }

  private static class MessageNullableRepeatedField extends MessageField {
    private final Descriptors.FieldDescriptor booleanDesc;

    public MessageNullableRepeatedField(Descriptors.FieldDescriptor booleanDesc, Descriptors.FieldDescriptor arrayDescriptor) {
      super(arrayDescriptor);
      Preconditions.checkArgument(arrayDescriptor.isRepeated());
      Preconditions.checkArgument(booleanDesc.getJavaType() == Descriptors.FieldDescriptor.JavaType.BOOLEAN);
      this.booleanDesc = booleanDesc;
    }

    @Override
    public boolean isRepeated() {
      return true;
    }

    @Override
    public boolean hasValue(Message message) {
      if (((Collection) message.getField(descriptor)).isEmpty()) {
        return message.hasField(booleanDesc) && (boolean) message.getField(booleanDesc);
      }
      return true;
    }

    @Override
    public Object getValue(Message message) {
      return message.getField(descriptor);
    }
  }

  static class MessageJsonDescriptor {
    private static final Map<Class<? extends Message>, MessageJsonDescriptor> BY_CLASS = new HashMap<>();
    private final MessageField[] fields;

    private MessageJsonDescriptor(MessageField[] fields) {
      this.fields = fields;
    }

    MessageField[] getFields() {
      return fields;
    }

    static MessageJsonDescriptor of(Message message) {
      MessageJsonDescriptor desc = BY_CLASS.get(message.getClass());
      if (desc == null) {
        desc = introspect(message);
        BY_CLASS.put(message.getClass(), desc);
      }
      return desc;
    }

    private static MessageJsonDescriptor introspect(Message message) {
      List<MessageField> fields = new ArrayList<>();
      BiMap<Descriptors.FieldDescriptor, Descriptors.FieldDescriptor> repeatedToBoolean = HashBiMap.create();
      for (Descriptors.FieldDescriptor desc : message.getDescriptorForType().getFields()) {
        if (desc.isRepeated()) {
          String booleanName = desc.getName() + "PresentIfEmpty";
          Descriptors.FieldDescriptor booleanDesc = message.getDescriptorForType().findFieldByName(booleanName);
          if (booleanDesc != null && booleanDesc.getJavaType() == Descriptors.FieldDescriptor.JavaType.BOOLEAN) {
            repeatedToBoolean.put(desc, booleanDesc);
          }
        }
      }
      for (Descriptors.FieldDescriptor descriptor : message.getDescriptorForType().getFields()) {
        if (descriptor.isRepeated()) {
          Descriptors.FieldDescriptor booleanDesc = repeatedToBoolean.get(descriptor);
          if (booleanDesc == null) {
            fields.add(new MessageRepeatedField(descriptor));
          } else {
            fields.add(new MessageNullableRepeatedField(booleanDesc, descriptor));
          }
        } else if (!repeatedToBoolean.containsValue(descriptor)) {
          fields.add(new MessageNonRepeatedField(descriptor));
        }
      }
      return new MessageJsonDescriptor(fields.toArray(new MessageField[fields.size()]));
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
    JsonWriter jsonWriter = JsonWriter.of(json);
    write(message, jsonWriter);
    return json.toString();
  }

  private static void writeMessage(Message message, JsonWriter writer) {
    MessageJsonDescriptor fields = MessageJsonDescriptor.of(message);
    for (MessageField field : fields.getFields()) {
      if (field.hasValue(message)) {
        writer.name(field.getName());
        if (field.isRepeated()) {
          writer.beginArray();
          for (Object o : (Collection) field.getValue(message)) {
            writeFieldValue(field, o, writer);
          }
          writer.endArray();
        } else {
          writeFieldValue(field, field.getValue(message), writer);
        }
      }
    }
  }

  private static void writeFieldValue(MessageField field, Object value, JsonWriter writer) {
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
      case ENUM:
        writer.value(((Descriptors.EnumValueDescriptor) value).getName());
        break;
      case MESSAGE:
        writer.beginObject();
        writeMessage((Message) value, writer);
        writer.endObject();
        break;
      default:
        throw new IllegalStateException(String.format("JSON format does not support type '%s' of field '%s'", field.getJavaType(), field.getName()));
    }
  }
}
