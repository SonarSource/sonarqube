/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.v2.api.dop.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.server.common.almsettings.permission.InstallationCheckStatus;
import org.sonar.server.common.almsettings.permission.PermissionCheckStatus;

/**
 * The fields below {@code checkedAt} are GitHub-only and optional: GitLab and Azure DevOps have no equivalent notion
 * and omit them entirely, and a GitHub check omits the count and the list whenever its installation scan did not
 * complete. An omitted field means "not known", never "nothing to approve" (SONAR-32166).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PermissionCheckResource(
  @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Key of the DevOps Platform configuration")
  String key,

  @Schema(description = "DevOps Platform type of the configuration (github, gitlab, azure)")
  String type,

  @Schema(description = "Whether the configuration grants the write permissions the Remediation Agent needs to clone and open pull requests")
  PermissionCheckStatus status,

  @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Epoch milliseconds when this status was last computed. May reflect a cached result.")
  long checkedAt,

  @Schema(accessMode = Schema.AccessMode.READ_ONLY, nullable = true, description = """
    GitHub only. Permissions that the GitHub App does not request. Add these permissions to the app before an
    installation can approve them. If the system cannot read the app configuration, 'status' is CHECK_FAILED. In this
    case, an empty list means that no app permission result is available.
    """)
  @Nullable List<PermissionDeficitResource> appMissingPermissions,

  @Schema(accessMode = Schema.AccessMode.READ_ONLY, nullable = true, description = """
    GitHub only. Whether the per-installation scan produced a trustworthy answer. The counts and the list are
    authoritative only when this is COMPLETE. On a project request, NOT_INSTALLED means the app is not installed on
    that project's repository — a definite answer, unlike FAILED. NOT_RUN means that the system did not check an
    installation because the project has no bound repository. In this case, 'status' covers only the app configuration.
    """)
  @Nullable InstallationCheckStatus installationCheckStatus,

  @Schema(accessMode = Schema.AccessMode.READ_ONLY, nullable = true, description = """
    GitHub administrator requests only. How many installations the app has at all. Tells an app nobody has installed
    apart from an app whose installations have all approved, which both report zero affected installations. Omitted
    unless 'installationCheckStatus' is COMPLETE.
    """)
  @Nullable Integer totalInstallationCount,

  @Schema(accessMode = Schema.AccessMode.READ_ONLY, nullable = true, description = """
    GitHub administrator requests only. Exact number of installations with missing permissions. Omitted unless
    'installationCheckStatus' is COMPLETE.
    """)
  @Nullable Integer affectedInstallationCount,

  @Schema(accessMode = Schema.AccessMode.READ_ONLY, nullable = true, description = """
    GitHub administrator requests only. Every installation with missing permissions, in a stable order. Omitted unless
    'installationCheckStatus' is COMPLETE.
    """)
  @Nullable List<AffectedInstallationResource> affectedInstallations
) {
}
