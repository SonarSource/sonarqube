/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.webhook;

import java.util.Objects;
import java.util.function.Supplier;
import org.sonar.api.config.Configuration;

import static java.util.Objects.requireNonNull;

public interface WebHooks {
  void sendProjectAnalysisUpdate(Configuration configuration, Analysis analysis, Supplier<WebhookPayload> payloadSupplier);

  final class Analysis {
    private final String projectUuid;
    private final String ceTaskUuid;

    public Analysis(String projectUuid, String ceTaskUuid) {
      this.projectUuid = requireNonNull(projectUuid, "projectUuid can't be null");
      this.ceTaskUuid = requireNonNull(ceTaskUuid, "ceTaskUuid can't be null");
    }

    public String getProjectUuid() {
      return projectUuid;
    }

    public String getCeTaskUuid() {
      return ceTaskUuid;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Analysis analysis = (Analysis) o;
      return projectUuid.equals(analysis.projectUuid) && ceTaskUuid.equals(analysis.ceTaskUuid);
    }

    @Override
    public int hashCode() {
      return Objects.hash(projectUuid, ceTaskUuid);
    }
  }
}
