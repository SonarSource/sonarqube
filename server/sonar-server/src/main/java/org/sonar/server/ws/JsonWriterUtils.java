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
package org.sonar.server.ws;

import java.util.Collection;
import java.util.Date;
import javax.annotation.Nullable;
import org.sonar.api.utils.text.JsonWriter;

public class JsonWriterUtils {

  private JsonWriterUtils() {
    // Utility class
  }

  public static void writeIfNeeded(JsonWriter json, @Nullable String value, String field, @Nullable Collection<String> fields) {
    if (isFieldNeeded(field, fields)) {
      json.prop(field, value);
    }
  }

  public static void writeIfNeeded(JsonWriter json, @Nullable Boolean value, String field, @Nullable Collection<String> fields) {
    if (isFieldNeeded(field, fields)) {
      json.prop(field, value);
    }
  }

  public static void writeIfNeeded(JsonWriter json, @Nullable Integer value, String field, @Nullable Collection<String> fields) {
    if (isFieldNeeded(field, fields)) {
      json.prop(field, value);
    }
  }

  public static void writeIfNeeded(JsonWriter json, @Nullable Long value, String field, @Nullable Collection<String> fields) {
    if (isFieldNeeded(field, fields)) {
      json.prop(field, value);
    }
  }

  public static void writeIfNeeded(JsonWriter json, @Nullable Date value, String field, @Nullable Collection<String> fields) {
    if (isFieldNeeded(field, fields)) {
      json.propDateTime(field, value);
    }
  }

  public static boolean isFieldNeeded(String field, @Nullable Collection<String> fields) {
    return fields == null || fields.isEmpty() || fields.contains(field);
  }
}
