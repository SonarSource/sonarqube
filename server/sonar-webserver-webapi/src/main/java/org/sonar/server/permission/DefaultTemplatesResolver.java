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
package org.sonar.server.permission;

import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.db.DbSession;
import org.sonar.db.permission.template.DefaultTemplates;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public interface DefaultTemplatesResolver {
  /**
   * Resolve the effective default templates uuid for the specified {@link DefaultTemplates}.
   * <ul>
   *   <li>{@link ResolvedDefaultTemplates#project} is always the same as {@link DefaultTemplates#projectUuid}</li>
   *   <li>when Governance is not installed, {@link ResolvedDefaultTemplates#application} is always {@code null}</li>
   *   <li>when Governance is installed, {@link ResolvedDefaultTemplates#application} is  {@link DefaultTemplates#applicationsUuid}
   *       when it is non {@code null}, otherwise it is {@link DefaultTemplates#projectUuid}</li>
   *   <li>when Governance is installed, {@link ResolvedDefaultTemplates#portfolio} is  {@link DefaultTemplates#portfoliosUuid}
   *       when it is non {@code null}, otherwise it is {@link DefaultTemplates#projectUuid}</li>
   * </ul>
   */
  ResolvedDefaultTemplates resolve(DbSession dbSession, DefaultTemplates defaultTemplates);

  @Immutable
  final class ResolvedDefaultTemplates {
    private final String project;
    private final String application;
    private final String portfolio;

    public ResolvedDefaultTemplates(String project, @Nullable String application, @Nullable String portfolio) {
      this.project = requireNonNull(project, "project can't be null");
      this.application = application;
      this.portfolio = portfolio;
    }

    public String getProject() {
      return project;
    }

    public Optional<String> getApplication() {
      return ofNullable(application);
    }

    public Optional<String> getPortfolio() {
      return ofNullable(portfolio);
    }
  }
}
