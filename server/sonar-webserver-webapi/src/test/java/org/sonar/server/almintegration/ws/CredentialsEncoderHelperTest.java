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
package org.sonar.server.almintegration.ws;

import java.util.Base64;
import org.junit.Test;
import org.sonar.db.alm.setting.ALM;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class CredentialsEncoderHelperTest {
  private static final String PAT = "pat";
  private static final String USERNAME = "user";

  @Test
  public void encodes_credential_returns_just_pat_for_non_bitbucketcloud() {
    assertThat(CredentialsEncoderHelper.encodeCredentials(ALM.GITHUB, PAT, null))
      .isEqualTo("pat");
  }

  @Test
  public void encodes_credential_returns_username_and_encoded_pat_for_bitbucketcloud() {
    String encodedPat = Base64.getEncoder().encodeToString((USERNAME + ":" + PAT).getBytes(UTF_8));

    String encodedCredential = CredentialsEncoderHelper.encodeCredentials(ALM.BITBUCKET_CLOUD, PAT, USERNAME);
    assertThat(encodedCredential)
      .doesNotContain(USERNAME)
      .doesNotContain(PAT)
      .contains(encodedPat);
  }

}
