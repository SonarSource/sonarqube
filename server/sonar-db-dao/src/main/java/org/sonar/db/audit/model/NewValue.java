/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.audit.model;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.sonar.db.component.ComponentQualifiers.APP;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;
import static org.sonar.db.component.ComponentQualifiers.VIEW;

public abstract class NewValue {

  protected void addField(StringBuilder sb, String field, @Nullable String value, boolean isString) {
    if (!isNullOrEmpty(value)) {
      sb.append(field);
      addQuote(sb, isString);
      if (value.contains("\"")) {
        value = value.replace("\"", "\\\"");
      }
      sb.append(value);
      addQuote(sb, isString);
      sb.append(", ");
    }
  }

  protected void endString(StringBuilder sb) {
    int length = sb.length();
    if (sb.length() > 1) {
      sb.delete(length - 2, length - 1);
    }
    sb.append("}");
  }

  private static void addQuote(StringBuilder sb, boolean isString) {
    if (isString) {
      sb.append("\"");
    }
  }

  @CheckForNull
  protected static String getQualifier(@Nullable String qualifier) {
    if (qualifier == null) {
      return null;
    }
    switch (qualifier) {
      case VIEW:
        return "portfolio";
      case APP:
        return "application";
      case PROJECT:
        return "project";
      default:
        return null;
    }
  }

  @Override
  public abstract String toString();
}
