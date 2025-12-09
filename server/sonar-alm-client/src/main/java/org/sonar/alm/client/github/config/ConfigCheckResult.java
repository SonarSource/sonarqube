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
package org.sonar.alm.client.github.config;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import javax.annotation.Nullable;

public record ConfigCheckResult(@SerializedName("application") ApplicationStatus application, @SerializedName("installations") List<InstallationStatus> installations) {

  public record ConfigStatus(@SerializedName("status") String status, @Nullable @SerializedName("errorMessage") String errorMessage) {

    public static final String SUCCESS_STATUS = "SUCCESS";
    public static final String FAILED_STATUS = "FAILED";

    public static final ConfigStatus SUCCESS = new ConfigStatus(SUCCESS_STATUS);

    public ConfigStatus(String status) {
      this(status, null);
    }

    public static ConfigStatus failed(String errorMessage) {
      return new ConfigStatus(FAILED_STATUS, errorMessage);
    }

  }

  public record ApplicationStatus(@SerializedName("jit") ConfigStatus jit, @SerializedName("autoProvisioning") ConfigStatus autoProvisioning) {
  }

  public record InstallationStatus(@SerializedName("organization") String organization, @SerializedName("jit") ConfigStatus jit,
    @SerializedName("autoProvisioning") ConfigStatus autoProvisioning) {
  }
}
