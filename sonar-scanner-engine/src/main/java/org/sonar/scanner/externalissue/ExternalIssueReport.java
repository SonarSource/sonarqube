/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import javax.annotation.Nullable;

public class ExternalIssueReport {
  Issue[] issues;
  Rule[] rules;

  static class Issue {
    @Nullable
    String engineId;
    @Nullable
    String ruleId;
    @Nullable
    String severity;
    @Nullable
    String type;
    @Nullable
    Integer effortMinutes;
    Location primaryLocation;
    @Nullable
    Location[] secondaryLocations;
  }

  static class Rule {
    String id;
    String engineId;
    String name;
    @Nullable
    String description;
    String cleanCodeAttribute;
    Impact[] impacts;
  }

  static class Impact {
    String severity;
    String softwareQuality;
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
