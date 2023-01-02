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
package org.sonar.scanner.externalissue.sarif;

class SarifImportResults {

  private final int successFullyImportedIssues;
  private final int successFullyImportedRuns;
  private final int failedRuns;

  SarifImportResults(int successFullyImportedIssues, int successFullyImportedRuns, int failedRuns) {
    this.successFullyImportedIssues = successFullyImportedIssues;
    this.successFullyImportedRuns = successFullyImportedRuns;
    this.failedRuns = failedRuns;
  }

  int getSuccessFullyImportedIssues() {
    return successFullyImportedIssues;
  }

  int getSuccessFullyImportedRuns() {
    return successFullyImportedRuns;
  }

  int getFailedRuns() {
    return failedRuns;
  }

  static SarifImportResultBuilder builder() {
    return new SarifImportResultBuilder();
  }

  static final class SarifImportResultBuilder {
    private int successFullyImportedIssues;
    private int successFullyImportedRuns;

    private int failedRuns;

    private SarifImportResultBuilder() {
    }

    SarifImportResultBuilder successFullyImportedIssues(int successFullyImportedIssues) {
      this.successFullyImportedIssues = successFullyImportedIssues;
      return this;
    }

    SarifImportResultBuilder successFullyImportedRuns(int successFullyImportedRuns) {
      this.successFullyImportedRuns = successFullyImportedRuns;
      return this;
    }

    SarifImportResultBuilder failedRuns(int failedRuns) {
      this.failedRuns = failedRuns;
      return this;
    }

    SarifImportResults build() {
      return new SarifImportResults(successFullyImportedIssues, successFullyImportedRuns, failedRuns);
    }
  }
}
