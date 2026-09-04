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
package org.sonar.alm.client.gitlab;

import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.auth.gitlab.GsonUser;
import org.sonar.db.alm.setting.AlmSettingDto;

import static com.google.common.base.Strings.isNullOrEmpty;

@ComputeEngineSide
@ServerSide
public class GitlabGlobalSettingsValidator {

  public enum ValidationMode {COMPLETE, AUTH_ONLY}

  // GitLab associates every Project/Group Access Token with a bot user. The "bot" flag on GET /user is the primary
  // signal; the username pattern is a fallback for older self-managed GitLab versions that may not expose it.
  // Documented variants: project_<id>_bot, project_<id>_bot<count>, project_<id>_bot_<random>, and group_ equivalents.
  // The suffix is anchored to those exact forms (not a bare \w*) so a human username that happens to start with
  // "bot" (e.g. "group_99_botanist") isn't misclassified as a bot token.
  private static final Pattern BOT_USERNAME_PATTERN = Pattern.compile("^(project|group)_\\d+_bot(\\d+|_[0-9a-f]+)?$");

  private static final String API_SCOPE = "api";

  private final Encryption encryption;
  private final GitlabApplicationClient gitlabApplicationClient;

  public GitlabGlobalSettingsValidator(GitlabApplicationClient gitlabApplicationClient, Settings settings) {
    this.encryption = settings.getEncryption();
    this.gitlabApplicationClient = gitlabApplicationClient;
  }

  public void validate(AlmSettingDto almSettingDto) {
    String gitlabUrl = almSettingDto.getUrl();
    String accessToken = almSettingDto.getDecryptedPersonalAccessToken(encryption);
    validate(ValidationMode.COMPLETE, gitlabUrl, accessToken);
  }

  public void validate(ValidationMode validationMode, @Nullable String gitlabApiUrl, @Nullable String accessToken) {
    if (gitlabApiUrl == null) {
      throw new IllegalArgumentException("Your Gitlab global configuration is incomplete. The GitLab URL must be set.");
    }
    gitlabApplicationClient.checkUrl(gitlabApiUrl);
    if (ValidationMode.AUTH_ONLY.equals(validationMode)) {
      return;
    }

    String decryptedToken = getDecryptedToken(accessToken);
    if (decryptedToken == null) {
      throw new IllegalArgumentException("Your Gitlab global configuration is incomplete. The GitLab access token must be set.");
    }
    gitlabApplicationClient.checkToken(gitlabApiUrl, decryptedToken);
    gitlabApplicationClient.checkReadPermission(gitlabApiUrl, decryptedToken);
    gitlabApplicationClient.checkWritePermission(gitlabApiUrl, decryptedToken);
  }

  /**
   * Whether the given configuration's token has the {@code api} scope, the only scope that grants the write access
   * the Remediation Agent needs (create branches, open merge requests). Introspects the token directly via
   * {@code GET /personal_access_tokens/self} rather than inferring it from a probe call's side effect: GitLab has
   * reclassified some endpoints (e.g. {@code /markdown}, used by {@link #validate(AlmSettingDto)}) as requiring only
   * {@code read_api}, which made that inference silently pass for a {@code read_api}-only token (SONAR-31861). Kept
   * separate from {@link #validate(AlmSettingDto)}, which must keep succeeding for a {@code read_api}-only token on
   * its existing consumers (PR decoration, validate_binding).
   */
  public boolean hasApiScope(AlmSettingDto almSettingDto) {
    String gitlabUrl = almSettingDto.getUrl();
    if (gitlabUrl == null) {
      throw new IllegalArgumentException("Your Gitlab global configuration is incomplete. The GitLab URL must be set.");
    }
    String decryptedToken = getDecryptedToken(almSettingDto.getDecryptedPersonalAccessToken(encryption));
    if (decryptedToken == null) {
      throw new IllegalArgumentException("Your Gitlab global configuration is incomplete. The GitLab access token must be set.");
    }
    GsonPersonalAccessTokenInfo tokenInfo = gitlabApplicationClient.getPersonalAccessTokenInfo(gitlabUrl, decryptedToken);
    if (tokenInfo == null) {
      return false;
    }
    List<String> scopes = tokenInfo.getScopes();
    return scopes != null && scopes.contains(API_SCOPE);
  }

  /**
   * Whether the given configuration's token belongs to a GitLab bot user, i.e. a Project/Group Access Token rather
   * than a genuine Personal Access Token. Bot tokens are fine for the DevOps Platform features SonarQube already
   * offers (PR decoration, etc.) — they only fail the Remediation Agent's token-minting flow (SONAR-31770), so this
   * is deliberately separate from {@link #validate(AlmSettingDto)}, which must keep succeeding for bot tokens.
   */
  public boolean isBotToken(AlmSettingDto almSettingDto) {
    String gitlabUrl = almSettingDto.getUrl();
    String accessToken = getDecryptedToken(almSettingDto.getDecryptedPersonalAccessToken(encryption));
    if (gitlabUrl == null || isNullOrEmpty(accessToken)) {
      return false;
    }
    GsonUser user = gitlabApplicationClient.checkToken(gitlabUrl, accessToken);
    if (user == null) {
      return false;
    }
    String username = user.getUsername();
    return user.isBot() || (username != null && BOT_USERNAME_PATTERN.matcher(username).matches());
  }

  @CheckForNull
  public String getDecryptedToken(@Nullable String token) {
    if (!isNullOrEmpty(token) && encryption.isEncrypted(token)) {
      return encryption.decrypt(token);
    }
    return token;
  }
}
