/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.core.sarif;

import com.fasterxml.jackson.core.JsonProcessingException;

public class SarifDeserializationException extends RuntimeException {

  public enum Category {
    SYNTAX("invalid JSON syntax or non-UTF-8 encoding"),
    VALUE("value overflow or type mismatch"),
    MAPPING("cannot map to SARIF schema"),
    FILE_NOT_FOUND("file not found");

    private final String description;

    Category(String description) {
      this.description = description;
    }

    public String description() {
      return description;
    }
  }

  private final Category category;
  private final int lineNumber;

  public SarifDeserializationException(Category category, String message, Throwable cause) {
    super(message, cause);
    this.category = category;
    this.lineNumber = extractLineNumber(cause);
  }

  public Category getCategory() {
    return category;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  private static int extractLineNumber(Throwable cause) {
    if (cause instanceof JsonProcessingException jsonException && jsonException.getLocation() != null) {
      return jsonException.getLocation().getLineNr();
    }
    return -1;
  }
}
