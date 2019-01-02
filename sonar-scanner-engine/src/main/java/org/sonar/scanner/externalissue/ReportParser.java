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
package org.sonar.scanner.externalissue;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

public class ReportParser {
  private Gson gson = new Gson();
  private Path filePath;

  public ReportParser(Path filePath) {
    this.filePath = filePath;
  }

  public Report parse() {
    try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
      return validate(gson.fromJson(reader, Report.class));
    } catch (JsonIOException | IOException e) {
      throw new IllegalStateException("Failed to read external issues report '" + filePath + "'", e);
    } catch (JsonSyntaxException e) {
      throw new IllegalStateException("Failed to read external issues report '" + filePath + "': invalid JSON syntax", e);
    }
  }

  private Report validate(Report report) {
    for (Issue issue : report.issues) {
      mandatoryField(issue.primaryLocation, "primaryLocation");
      mandatoryField(issue.engineId, "engineId");
      mandatoryField(issue.ruleId, "ruleId");
      mandatoryField(issue.severity, "severity");
      mandatoryField(issue.type, "type");
      mandatoryField(issue.primaryLocation, "primaryLocation");
      mandatoryFieldPrimaryLocation(issue.primaryLocation.filePath, "filePath");
      mandatoryFieldPrimaryLocation(issue.primaryLocation.message, "message");

      if (issue.primaryLocation.textRange != null) {
        mandatoryFieldPrimaryLocation(issue.primaryLocation.textRange.startLine, "startLine of the text range");
      }
      
      if (issue.secondaryLocations != null) {
        for (Location l : issue.secondaryLocations) {
          mandatoryFieldSecondaryLocation(l.filePath, "filePath");
          mandatoryFieldSecondaryLocation(l.textRange, "textRange");
          mandatoryFieldSecondaryLocation(l.textRange.startLine, "startLine of the text range");
        }
      }
    }

    return report;
  }

  private void mandatoryFieldPrimaryLocation(@Nullable Object value, String fieldName) {
    if (value == null) {
      throw new IllegalStateException(String.format("Failed to parse report '%s': missing mandatory field '%s' in the primary location of the issue.", filePath, fieldName));
    }
  }
  
  private void mandatoryFieldSecondaryLocation(@Nullable Object value, String fieldName) {
    if (value == null) {
      throw new IllegalStateException(String.format("Failed to parse report '%s': missing mandatory field '%s' in a secondary location of the issue.", filePath, fieldName));
    }
  }

  private void mandatoryField(@Nullable Object value, String fieldName) {
    if (value == null) {
      throw new IllegalStateException(String.format("Failed to parse report '%s': missing mandatory field '%s'.", filePath, fieldName));
    }
  }

  private void mandatoryField(@Nullable String value, String fieldName) {
    if (StringUtils.isBlank(value)) {
      throw new IllegalStateException(String.format("Failed to parse report '%s': missing mandatory field '%s'.", filePath, fieldName));
    }
  }

  static class Report {
    Issue[] issues;
  }

  static class Issue {
    String engineId;
    String ruleId;
    String severity;
    String type;
    @Nullable
    Integer effortMinutes;
    Location primaryLocation;
    @Nullable
    Location[] secondaryLocations;
  }

  static class Location {
    @Nullable
    String message;
    String filePath;
    @Nullable
    TextRange textRange;
  }

  static class TextRange {
    Integer startLine;
    @Nullable
    Integer startColumn;
    @Nullable
    Integer endLine;
    @Nullable
    Integer endColumn;
  }
}
