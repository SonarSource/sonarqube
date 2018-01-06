/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.config.Configuration;

import static java.util.Objects.requireNonNull;

public interface WebHooks {
  /**
   * Tells whether any webHook is configured at all for the specified {@link Configuration}.
   *
   * <p>
   * This can be used to not do consuming operations before calling
   * {@link #sendProjectAnalysisUpdate(Configuration, Analysis, Supplier)}
   */
  boolean isEnabled(Configuration configuration);

  /**
   * Calls all WebHooks configured in the specified {@link Configuration} for the specified analysis with the
   * {@link WebhookPayload} provided by the specified Supplier.
   */
  void sendProjectAnalysisUpdate(Configuration configuration, Analysis analysis, Supplier<WebhookPayload> payloadSupplier);

  final class Analysis {
    private final String projectUuid;
    private final String ceTaskUuid;
    private final String analysisUuid;

    public Analysis(String projectUuid, @Nullable String analysisUuid, @Nullable  String ceTaskUuid) {
      this.projectUuid = requireNonNull(projectUuid, "projectUuid can't be null");
      this.analysisUuid = analysisUuid;
      this.ceTaskUuid = ceTaskUuid;
    }

    public String getProjectUuid() {
      return projectUuid;
    }

    @CheckForNull
    public String getCeTaskUuid() {
      return ceTaskUuid;
    }

    @CheckForNull
    public String getAnalysisUuid() {
      return analysisUuid;
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
      return Objects.equals(projectUuid, analysis.projectUuid) &&
        Objects.equals(ceTaskUuid, analysis.ceTaskUuid) &&
        Objects.equals(analysisUuid, analysis.analysisUuid);
    }

    @Override
    public int hashCode() {
      return Objects.hash(projectUuid, ceTaskUuid, analysisUuid);
    }

    @Override
    public String toString() {
      return "Analysis{" +
        "projectUuid='" + projectUuid + '\'' +
        ", ceTaskUuid='" + ceTaskUuid + '\'' +
        '}';
    }
  }
}
