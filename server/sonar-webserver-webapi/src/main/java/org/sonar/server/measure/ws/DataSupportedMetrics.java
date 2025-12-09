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
package org.sonar.server.measure.ws;

import java.util.Set;

import static org.sonar.api.measures.CoreMetrics.MAINTAINABILITY_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_ISSUES_KEY;


/**
 * This class contains the list of metrics that are supported in the web-api for the data type.
 */
public class DataSupportedMetrics {

  public static final Set<String> IMPACTS_SUPPORTED_METRICS = Set.of(
    SECURITY_ISSUES_KEY,
    MAINTAINABILITY_ISSUES_KEY,
    RELIABILITY_ISSUES_KEY,
    NEW_SECURITY_ISSUES_KEY,
    NEW_MAINTAINABILITY_ISSUES_KEY,
    NEW_RELIABILITY_ISSUES_KEY);

  private DataSupportedMetrics() {
    // only static methods
  }
}
