/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.alm.client.github.scanning.alert;

import com.google.gson.annotations.SerializedName;
import javax.annotation.Nullable;

public class GithubScanningAlertState {
  protected static final String GH_OPEN = "open";
  protected static final String GH_DISMISSED = "dismissed";
  protected static final String GH_WONTFIX = "won't fix";
  protected static final String GH_FALSE_POSITIVE = "false positive";

  @SerializedName("state")
  private String state;
  @SerializedName("dismissed_reason")
  private String dismissedReason;

  public GithubScanningAlertState(String state, @Nullable String dismissedReason) {
    this.state = state;
    this.dismissedReason = dismissedReason;
  }
}
