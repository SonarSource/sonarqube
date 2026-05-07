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
package org.sonar.server.v2.api.gitlab.config.resource;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.server.v2.api.model.ProvisioningType;

public record GitlabConfigurationResource(

  @Schema(accessMode = Schema.AccessMode.READ_ONLY)
  String id,

  boolean enabled,

  @Schema(implementation = String.class, description = "Gitlab Application id")
  String applicationId,

  @Schema(description = "Url of Gitlab instance for authentication (for instance https://gitlab.com/api/v4)")
  String url,

  boolean synchronizeGroups,

  @Schema(description = "Root Gitlab groups allowed to authenticate and provisioned. Ignored when allowAllGroups is true.")
  List<String> allowedGroups,

  @Schema(description = """
    When true with Auto-provisioning, every group visible to the provisioning token is provisioned \
    and the allowedGroups list is ignored. Has no effect with Just-in-Time provisioning. \
    Security risk: any user belonging to any group accessible by the provisioning token will be granted access. \
    Restrict access using allowedGroups unless broad access is intentional. \
    When using GitLab.com, be especially careful — unlike a self-managed instance, the provisioning token may have \
    visibility into a much larger number of groups, greatly increasing the attack surface. \
    Performance note: login may be slower for users belonging to a large number of groups, \
    as all their groups must be fetched from GitLab on every authentication.""")
  boolean allowAllGroups,

  boolean allowUsersToSignUp,

  ProvisioningType provisioningType,

  @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Whether or not the provisioningToken is defined")
  boolean isProvisioningTokenSet,

  @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "In case the GitLab configuration is incorrect, error message")
  @Nullable
  String errorMessage
) {
}

