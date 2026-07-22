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
package org.sonar.auth.github;

import java.util.Map;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GithubAppPermissionsTest {

  /**
   * An app created from the manifest must always pass {@code checkAppPermissions}: every required
   * permission must be requested with at least the same access level in the manifest.
   */
  @Test
  public void manifestPermissions_containAllRequiredPermissions() {
    for (Map.Entry<String, String> required : GithubAppPermissions.REQUIRED_PERMISSIONS.entrySet()) {
      assertThat(GithubAppPermissions.MANIFEST_PERMISSIONS)
        .as("manifest must request required permission '%s'", required.getKey())
        .containsEntry(required.getKey(), required.getValue());
    }
  }

  /**
   * The token-minting check must always pass {@code checkAppPermissions} too, for the same reason.
   */
  @Test
  public void tokenMintingPermissions_containAllRequiredPermissions() {
    for (Map.Entry<String, String> required : GithubAppPermissions.REQUIRED_PERMISSIONS.entrySet()) {
      assertThat(GithubAppPermissions.TOKEN_MINTING_PERMISSIONS)
        .as("token minting must require permission '%s'", required.getKey())
        .containsEntry(required.getKey(), required.getValue());
    }
  }

  @Test
  public void tokenMintingPermissions_requireContentsWrite() {
    assertThat(GithubAppPermissions.TOKEN_MINTING_PERMISSIONS).containsEntry("contents", GithubAppPermissions.WRITE);
  }

  @Test
  public void manifestPermissions_requestContentsWrite() {
    assertThat(GithubAppPermissions.MANIFEST_PERMISSIONS).containsEntry("contents", GithubAppPermissions.WRITE);
  }

  @Test
  public void manifestEvents_areEmptyByDefault() {
    // No webhook events are required for PR analysis or provisioning.
    assertThat(GithubAppPermissions.MANIFEST_EVENTS).isEmpty();
  }
}
