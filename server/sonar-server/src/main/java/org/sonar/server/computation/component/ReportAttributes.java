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
package org.sonar.server.computation.component;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public class ReportAttributes {
  private final int ref;
  private final String version;

  public ReportAttributes(int ref, @Nullable String version) {
    this.ref = ref;
    this.version = version;
  }

  /**
   * The component ref in the batch report.
   */
  public int getRef() {
    return ref;
  }

  /**
   * The project or module version as defined in the batch report.
   */
  @CheckForNull
  public String getVersion() {
    return this.version;
  }

  @Override
  public String toString() {
    return "ReportAttributes{" +
      "ref=" + ref +
      ", version='" + version + '\'' +
      '}';
  }
}
