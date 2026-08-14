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
package org.sonar.server.common.almsettings.azuredevops;

import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.alm.client.azure.AzureDevOpsValidator;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.core.scm.ScmAccessToken;
import org.sonar.core.scm.ScmAccessTokenProvider;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Azure DevOps counterpart to {@code GitlabScmAccessTokenProvider} (SONAR-31165) — but unlike GitLab
 * and GitHub, Azure DevOps exposes no PAT-minting API reachable with PAT authentication (its token
 * lifecycle API requires an Entra ID OAuth bearer token). There is therefore nothing to mint: this
 * provider passes through the instance-level Personal Access Token already stored on the project's
 * bound {@code AlmSettingDto} (the same one {@code AzureDevOpsValidator} validates for the
 * create/update-Azure-settings web actions), after re-validating it so a stale/revoked PAT fails fast
 * here rather than opaquely during a later git push or PR creation.
 *
 * <p>Follows the same self-filtering-delegate convention as {@code AzureDevOpsProjectCreatorFactory}:
 * returns {@code Optional.empty()} whenever the project isn't bound to Azure DevOps, rather than
 * throwing.
 */
@ServerSide
public class AzureDevOpsScmAccessTokenProvider implements ScmAccessTokenProvider {

  private static final Logger LOG = LoggerFactory.getLogger(AzureDevOpsScmAccessTokenProvider.class);
  private static final Pattern CRLF_PATTERN = Pattern.compile("[\r\n]");

  /**
   * Azure Repos ignores the username in a git-remote credential URL ({@code https://<user>:<pat>@host}),
   * so the PAT is passed as the sole credential.
   */
  private static final String GIT_USERNAME = "";

  private final DbClient dbClient;
  private final AzureDevOpsValidator azureDevOpsValidator;
  private final Encryption encryption;

  public AzureDevOpsScmAccessTokenProvider(DbClient dbClient, AzureDevOpsValidator azureDevOpsValidator, Settings settings) {
    this.dbClient = dbClient;
    this.azureDevOpsValidator = azureDevOpsValidator;
    this.encryption = settings.getEncryption();
  }

  @Override
  public Optional<ScmAccessToken> mint(String projectKey) {
    String safeProjectKey = sanitizeForLog(projectKey);
    Optional<AlmSettingDto> resolvedAlmSetting;
    try (DbSession dbSession = dbClient.openSession(false)) {
      resolvedAlmSetting = resolveAzureDevOpsAlmSetting(dbSession, projectKey, safeProjectKey);
    }

    // Validation below is network I/O (an Azure DevOps API call), deliberately made outside the
    // DbSession above — see GitlabScmAccessTokenProvider for the same rationale.
    return resolvedAlmSetting.map(almSetting -> passThrough(safeProjectKey, almSetting));
  }

  /**
   * Walks project -&gt; its DevOps Platform binding -&gt; the bound {@link AlmSettingDto}, short-circuiting
   * to {@link Optional#empty()} (with a warning) at whichever step is missing, or once the binding
   * turns out not to be Azure DevOps.
   */
  private Optional<AlmSettingDto> resolveAzureDevOpsAlmSetting(DbSession dbSession, String projectKey, String safeProjectKey) {
    Optional<ProjectDto> project = dbClient.projectDao().selectProjectByKey(dbSession, projectKey);
    if (project.isEmpty()) {
      LOG.warn("Cannot provide an Azure DevOps access token: unknown project '{}'", safeProjectKey);
      return Optional.empty();
    }

    Optional<ProjectAlmSettingDto> projectAlmSetting = dbClient.projectAlmSettingDao().selectByProject(dbSession, project.get());
    if (projectAlmSetting.isEmpty()) {
      LOG.warn("Cannot provide an Azure DevOps access token: project '{}' is not bound to any DevOps Platform", safeProjectKey);
      return Optional.empty();
    }

    return dbClient.almSettingDao().selectByUuid(dbSession, projectAlmSetting.get().getAlmSettingUuid())
      .filter(almSetting -> almSetting.getAlm() == ALM.AZURE_DEVOPS);
  }

  private ScmAccessToken passThrough(String safeProjectKey, AlmSettingDto almSetting) {
    // AzureDevOpsValidator.validate() can fail with either IllegalArgumentException (bad config) or
    // NullPointerException (missing URL/PAT via requireNonNull) — caught here as RuntimeException,
    // rather than naming NullPointerException explicitly, to avoid catching it as a control-flow signal.
    try {
      azureDevOpsValidator.validate(almSetting);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(
        format("Cannot provide an Azure DevOps access token for project '%s': invalid Azure DevOps configuration: %s", safeProjectKey, e.getMessage()), e);
    }

    String personalAccessToken = requireNonNull(almSetting.getDecryptedPersonalAccessToken(encryption), "Azure DevOps personal access token cannot be null");
    // Azure DevOps PATs are long-lived, admin-managed credentials with their own separate expiry — not
    // minted per call — so there is no per-request expiry to report here.
    return new ScmAccessToken(ALM.AZURE_DEVOPS.getId(), GIT_USERNAME, personalAccessToken, null);
  }

  /**
   * Strips CR/LF from the user-controlled project key before logging it, so a crafted value cannot
   * forge extra log lines/entries (CWE-117).
   */
  private static String sanitizeForLog(String value) {
    return CRLF_PATTERN.matcher(value).replaceAll("_");
  }
}
