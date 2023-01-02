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
package org.sonar.scanner.report;

import static java.util.Objects.requireNonNull;

public class CeTaskReportDataHolder {
  private boolean initialized = false;
  private String ceTaskId = null;
  private String ceTaskUrl = null;
  private String dashboardUrl= null;

  public void init(String ceTaskId, String ceTaskUrl, String dashboardUrl) {
    requireNonNull(ceTaskId, "CE task id must not be null");
    requireNonNull(ceTaskUrl, "CE task url must not be null");
    requireNonNull(dashboardUrl, "Dashboard url map must not be null");

    this.ceTaskId = ceTaskId;
    this.ceTaskUrl = ceTaskUrl;
    this.dashboardUrl = dashboardUrl;

    this.initialized = true;
  }

  private void verifyInitialized() {
    if (!initialized) {
      throw new IllegalStateException("Scan report hasn't been published yet");
    }
  }

  public String getCeTaskId() {
    verifyInitialized();
    return ceTaskId;
  }

  public String getDashboardUrl() {
    verifyInitialized();
    return dashboardUrl;
  }

  public String getCeTaskUrl() {
    verifyInitialized();
    return ceTaskUrl;
  }
}
