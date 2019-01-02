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
package org.sonar.scanner.scan.branch;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Container class for information about a pull request.
 */
@Immutable
public class PullRequestInfo {
  private final String key;
  private final String branch;
  private final String base;
  private final long analysisDate;

  public PullRequestInfo(String key, String branch, @Nullable String base, long analysisDate) {
    this.key = key;
    this.branch = branch;
    this.base = base;
    this.analysisDate = analysisDate;
  }

  public String getKey() {
    return key;
  }

  public String getBranch() {
    return branch;
  }

  @CheckForNull
  public String getBase() {
    return base;
  }

  public long getAnalysisDate() {
    return analysisDate;
  }
}
