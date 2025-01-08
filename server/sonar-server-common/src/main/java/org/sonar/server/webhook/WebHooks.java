/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.config.Configuration;
import org.sonar.db.project.ProjectDto;

import static java.util.Objects.requireNonNull;

public interface WebHooks {

  /**
   * Tells whether any webHook is configured for the specified {@link Configuration}.
   *
   * <p>
   * This can be used to not do consuming operations before calling
   * {@link #sendProjectAnalysisUpdate(Analysis, Supplier, PostProjectAnalysisTask.LogStatistics)}
   */
  boolean isEnabled(ProjectDto projectDto);

  /**
   * Calls all WebHooks configured in the specified {@link Configuration} for the specified analysis with the
   * {@link WebhookPayload} provided by the specified Supplier.
   */
  void sendProjectAnalysisUpdate(Analysis analysis, Supplier<WebhookPayload> payloadSupplier);

  /**
   * Override to be called from a {@link PostProjectAnalysisTask} implementation.
   */
  void sendProjectAnalysisUpdate(Analysis analysis, Supplier<WebhookPayload> payloadSupplier, PostProjectAnalysisTask.LogStatistics taskLogStatistics);

  record Analysis(String projectUuid, String analysisUuid, String ceTaskUuid) {
    public Analysis(String projectUuid, @Nullable String analysisUuid, @Nullable String ceTaskUuid) {
      this.projectUuid = requireNonNull(projectUuid, "projectUuid can't be null");
      this.analysisUuid = analysisUuid;
      this.ceTaskUuid = ceTaskUuid;
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
