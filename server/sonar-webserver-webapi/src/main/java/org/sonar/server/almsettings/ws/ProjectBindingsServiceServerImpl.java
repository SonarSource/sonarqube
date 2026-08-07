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
package org.sonar.server.almsettings.ws;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.alm.client.azure.AzureDevOpsHttpClient;
import org.sonar.alm.client.azure.GsonAzureRepo;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient;
import org.sonar.alm.client.bitbucketserver.BitbucketServerRestClient;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.alm.client.gitlab.GitlabApplicationClient;
import org.sonar.alm.client.gitlab.Project;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.auth.github.AppInstallationToken;
import org.sonar.auth.github.GithubAppConfiguration;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonarsource.dop.translation.api.ProjectBindingsQuery;
import org.sonarsource.dop.translation.api.ProjectBindingsService;
import org.sonarsource.dop.translation.api.model.PageRestResponse;
import org.sonarsource.dop.translation.api.model.ProjectBinding;
import org.sonarsource.dop.translation.api.model.ProjectBindings;
import org.sonarsource.organizations.server.DefaultOrganizationProvider;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * sonar-enterprise's own implementation of dop-translation's {@link ProjectBindingsService}, hosting live-fallback
 * resolution directly here rather than as a pure DB-read stub — see {@code dop-translation-url} tracker's
 * {@code plan/unclear.md} (decisions 2-3) for the rationale.
 */
public class ProjectBindingsServiceServerImpl implements ProjectBindingsService {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectBindingsServiceServerImpl.class);
  private static final Pattern CRLF_PATTERN = Pattern.compile("[\r\n]");
  private static final String URL_CANNOT_BE_NULL = "DevOps Platform url cannot be null";
  private static final String ALM_REPO_CANNOT_BE_NULL = "almRepo cannot be null";
  private static final String ALM_SLUG_CANNOT_BE_NULL = "almSlug cannot be null";
  private static final String PAT_CANNOT_BE_NULL = "Personal access token cannot be null";
  private static final String CLIENT_ID_CANNOT_BE_NULL = "clientId cannot be null";
  private static final String CLIENT_SECRET_CANNOT_BE_NULL = "clientSecret cannot be null";
  private static final String WORKSPACE_CANNOT_BE_NULL = "workspace cannot be null";

  private final DbClient dbClient;
  private final GithubApplicationClient githubApplicationClient;
  private final GithubGlobalSettingsValidator githubGlobalSettingsValidator;
  private final GitlabApplicationClient gitlabApplicationClient;
  private final AzureDevOpsHttpClient azureDevOpsHttpClient;
  private final BitbucketServerRestClient bitbucketServerRestClient;
  private final BitbucketCloudRestClient bitbucketCloudRestClient;
  private final Encryption encryption;

  public ProjectBindingsServiceServerImpl(DbClient dbClient, GithubApplicationClient githubApplicationClient,
    GithubGlobalSettingsValidator githubGlobalSettingsValidator, GitlabApplicationClient gitlabApplicationClient,
    AzureDevOpsHttpClient azureDevOpsHttpClient, BitbucketServerRestClient bitbucketServerRestClient,
    BitbucketCloudRestClient bitbucketCloudRestClient, Settings settings) {
    this.dbClient = dbClient;
    this.githubApplicationClient = githubApplicationClient;
    this.githubGlobalSettingsValidator = githubGlobalSettingsValidator;
    this.gitlabApplicationClient = gitlabApplicationClient;
    this.azureDevOpsHttpClient = azureDevOpsHttpClient;
    this.bitbucketServerRestClient = bitbucketServerRestClient;
    this.bitbucketCloudRestClient = bitbucketCloudRestClient;
    this.encryption = settings.getEncryption();
  }

  @Override
  public ProjectBinding getProjectBinding(String id) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectAlmSettingDto projectAlmSetting = dbClient.projectAlmSettingDao().selectByUuid(dbSession, id)
        .orElseThrow(() -> new NotFoundException(format("Project binding '%s' not found", sanitizeForLog(id))));
      return toProjectBinding(dbSession, projectAlmSetting, new HashMap<>());
    }
  }

  @Override
  public ProjectBindings searchProjectBindings(ProjectBindingsQuery query) {
    validateQuery(query);
    try (DbSession dbSession = dbClient.openSession(false)) {
      // Scoped to this one request: a Bitbucket Cloud OAuth token is workspace-wide, not per-repository, so
      // reusing it across every binding of the same ALM setting in a single search avoids minting one fresh
      // token per binding (e.g. an organizationId search backfilling many pre-existing bindings at once).
      Map<String, String> bitbucketCloudTokenCache = new HashMap<>();
      List<ProjectBinding> bindings = resolveMatches(dbSession, query).stream()
        .map(projectAlmSetting -> toProjectBindingOrSkip(dbSession, projectAlmSetting, bitbucketCloudTokenCache))
        .filter(Objects::nonNull)
        .toList();
      return new ProjectBindings(bindings, new PageRestResponse(1, bindings.size(), bindings.size()));
    }
  }

  /**
   * Same as {@link #toProjectBinding}, but never lets one binding's failure (e.g. a concurrently deleted ALM
   * setting) abort the whole search — unlike {@link #getProjectBinding}, which is about exactly one binding and
   * can afford to let such an inconsistency surface as an error.
   */
  @Nullable
  private ProjectBinding toProjectBindingOrSkip(DbSession dbSession, ProjectAlmSettingDto projectAlmSetting, Map<String, String> bitbucketCloudTokenCache) {
    try {
      return toProjectBinding(dbSession, projectAlmSetting, bitbucketCloudTokenCache);
    } catch (Exception e) {
      // Same rationale as resolveLive/persist: never log "e" directly, since an exception reaching this catch
      // could in principle carry ALM-influenced content from a lower layer.
      LOG.warn("Skipping project binding '{}' from search results: failed to build it: {} ({})", projectAlmSetting.getUuid(),
        sanitizeForLog(String.valueOf(e.getMessage())), e.getClass().getSimpleName());
      return null;
    }
  }

  private static void validateQuery(ProjectBindingsQuery query) {
    boolean hasProjectId = isNotBlank(query.projectId());
    boolean hasUrl = isNotBlank(query.url());
    boolean hasOrganizationId = isNotBlank(query.organizationId());
    boolean hasDevOpsPlatform = isNotBlank(query.devOpsPlatform());
    boolean hasRepositoryId = isNotBlank(query.repositoryId());

    if (hasDevOpsPlatform != hasRepositoryId) {
      throw BadRequestException.create("devOpsPlatform and repositoryId must be provided together");
    }
    int paramCount = (hasProjectId ? 1 : 0) + (hasUrl ? 1 : 0) + (hasOrganizationId ? 1 : 0) + (hasDevOpsPlatform ? 1 : 0);
    if (paramCount > 1) {
      throw BadRequestException.create("Only one of projectId, url, organizationId, or devOpsPlatform+repositoryId can be provided");
    }
    if (paramCount == 0) {
      throw BadRequestException.create("One of projectId, url, organizationId, or devOpsPlatform+repositoryId must be provided");
    }
  }

  private List<ProjectAlmSettingDto> resolveMatches(DbSession dbSession, ProjectBindingsQuery query) {
    if (isNotBlank(query.projectId())) {
      return dbClient.projectAlmSettingDao().selectByProject(dbSession, query.projectId()).map(List::of).orElseGet(List::of);
    }
    if (isNotBlank(query.url())) {
      return dbClient.projectAlmSettingDao().selectByUrl(dbSession, query.url());
    }
    if (isNotBlank(query.devOpsPlatform()) && isNotBlank(query.repositoryId())) {
      return resolveByDevOpsPlatformAndRepositoryId(dbSession, query.devOpsPlatform(), query.repositoryId());
    }
    if (isNotBlank(query.organizationId())) {
      return resolveByOrganizationId(dbSession, query.organizationId());
    }
    return List.of();
  }

  private List<ProjectAlmSettingDto> resolveByDevOpsPlatformAndRepositoryId(DbSession dbSession, String devOpsPlatform, String repositoryId) {
    ALM alm = fromDevOpsPlatform(devOpsPlatform);
    if (alm == null) {
      return List.of();
    }
    return dbClient.almSettingDao().selectByAlm(dbSession, alm).stream()
      .flatMap(almSetting -> dbClient.projectAlmSettingDao().selectByAlmSettingAndRepoIds(dbSession, almSetting, Set.of(repositoryId)).stream())
      .toList();
  }

  private List<ProjectAlmSettingDto> resolveByOrganizationId(DbSession dbSession, String organizationId) {
    if (!organizationId.equals(DefaultOrganizationProvider.ID.toString())) {
      return List.of();
    }
    return Arrays.stream(ALM.values())
      .flatMap(alm -> dbClient.projectAlmSettingDao().selectByAlm(dbSession, alm).stream())
      .toList();
  }

  private ProjectBinding toProjectBinding(DbSession dbSession, ProjectAlmSettingDto projectAlmSetting, Map<String, String> bitbucketCloudTokenCache) {
    AlmSettingDto almSetting = dbClient.almSettingDao().selectByUuid(dbSession, projectAlmSetting.getAlmSettingUuid())
      .orElseThrow(() -> new IllegalStateException(format("DevOps Platform setting with uuid '%s' cannot be found", projectAlmSetting.getAlmSettingUuid())));
    LiveResolution resolution = resolveUrlAndRepoId(dbSession, almSetting, projectAlmSetting, bitbucketCloudTokenCache);
    return new ProjectBinding(
      projectAlmSetting.getUuid(),
      projectAlmSetting.getProjectUuid(),
      toDevOpsPlatform(almSetting.getAlm()),
      resolution.repoId(),
      resolution.url())
      .slug(toSlug(almSetting.getAlm(), projectAlmSetting));
  }

  private LiveResolution resolveUrlAndRepoId(DbSession dbSession, AlmSettingDto almSetting, ProjectAlmSettingDto projectAlmSetting,
    Map<String, String> bitbucketCloudTokenCache) {
    String storedUrl = projectAlmSetting.getUrl();
    String storedRepoId = projectAlmSetting.getRepoId();
    if (isNotBlank(storedUrl) && isNotBlank(storedRepoId)) {
      return new LiveResolution(storedUrl, storedRepoId);
    }

    LiveResolution resolved = resolveLive(almSetting, projectAlmSetting, bitbucketCloudTokenCache);
    if (isNotBlank(resolved.url()) && isNotBlank(resolved.repoId())) {
      persist(dbSession, projectAlmSetting, resolved);
    }
    return resolved;
  }

  private void persist(DbSession dbSession, ProjectAlmSettingDto projectAlmSetting, LiveResolution resolved) {
    try {
      dbClient.projectAlmSettingDao().updateUrlAndRepoId(dbSession, projectAlmSetting.getUuid(), resolved.url(), resolved.repoId());
      dbSession.commit();
    } catch (Exception e) {
      // Not logging "e" directly: some JDBC drivers embed the failing bind value in a constraint/type-violation
      // message, and resolved.url()/resolved.repoId() originate from ALM API responses — see the equivalent
      // comment in resolveLive for why the raw throwable never reaches the logger in this class.
      LOG.warn("Failed to persist resolved url/repoId for DevOps Platform binding '{}': {} ({})", projectAlmSetting.getUuid(),
        sanitizeForLog(String.valueOf(e.getMessage())), e.getClass().getSimpleName());
      // Rolls the session back to a usable state: on databases such as PostgreSQL, an aborted statement
      // poisons the transaction until an explicit rollback, which would otherwise break every later read
      // on this same session (e.g. the next binding in a searchProjectBindings batch).
      dbSession.rollback();
    }
  }

  private LiveResolution resolveLive(AlmSettingDto almSetting, ProjectAlmSettingDto projectAlmSetting, Map<String, String> bitbucketCloudTokenCache) {
    try {
      return switch (almSetting.getAlm()) {
        case GITHUB -> resolveGithub(almSetting, projectAlmSetting);
        case GITLAB -> resolveGitlab(almSetting, projectAlmSetting);
        case AZURE_DEVOPS -> resolveAzure(almSetting, projectAlmSetting);
        case BITBUCKET -> resolveBitbucketServer(almSetting, projectAlmSetting);
        case BITBUCKET_CLOUD -> resolveBitbucketCloud(almSetting, projectAlmSetting, bitbucketCloudTokenCache);
      };
    } catch (Exception e) {
      // Never logs "e" directly: exceptions thrown by the ALM REST clients themselves (not just this class) can
      // embed raw, externally-influenced data (an ALM-side error response body, a repository identifier) in their
      // message chain — logging the throwable as-is would re-open the same CRLF log-injection issue that
      // sanitizeForLog exists to close, just one layer down, in code this class doesn't control.
      LOG.warn("Failed to resolve url/repoId for DevOps Platform binding '{}': {} ({})", projectAlmSetting.getUuid(),
        sanitizeForLog(String.valueOf(e.getMessage())), e.getClass().getSimpleName());
      return new LiveResolution("", "");
    }
  }

  private LiveResolution resolveGithub(AlmSettingDto almSetting, ProjectAlmSettingDto projectAlmSetting) {
    String almRepo = requireNonNull(projectAlmSetting.getAlmRepo(), ALM_REPO_CANNOT_BE_NULL);
    String safeAlmRepo = sanitizeForLog(almRepo);
    String url = requireNonNull(almSetting.getUrl(), URL_CANNOT_BE_NULL);
    GithubAppConfiguration githubAppConfiguration = githubGlobalSettingsValidator.validate(almSetting);
    long installationId = githubApplicationClient.getInstallationId(githubAppConfiguration, almRepo)
      .orElseThrow(() -> new IllegalStateException(format("GitHub App is not installed on repository '%s'", safeAlmRepo)));
    AppInstallationToken accessToken = githubApplicationClient.createAppInstallationToken(githubAppConfiguration, installationId, bareRepositoryName(almRepo))
      .orElseThrow(() -> new IllegalStateException(format("Failed to create a GitHub App installation token for repository '%s'", safeAlmRepo)));
    GithubApplicationClient.Repository repository = githubApplicationClient.getRepository(url, accessToken, almRepo)
      .orElseThrow(() -> new IllegalStateException(format("Repository '%s' not found on GitHub", safeAlmRepo)));
    String repoUrl = requireNonNull(repository.getUrl(), format("GitHub returned no url for repository '%s'", safeAlmRepo));
    return new LiveResolution(repoUrl, Long.toString(repository.getId()));
  }

  private LiveResolution resolveGitlab(AlmSettingDto almSetting, ProjectAlmSettingDto projectAlmSetting) {
    String pat = requireNonNull(almSetting.getDecryptedPersonalAccessToken(encryption), PAT_CANNOT_BE_NULL);
    String url = requireNonNull(almSetting.getUrl(), URL_CANNOT_BE_NULL);
    String almRepo = requireNonNull(projectAlmSetting.getAlmRepo(), ALM_REPO_CANNOT_BE_NULL);
    long gitlabProjectId;
    try {
      gitlabProjectId = Long.parseLong(almRepo);
    } catch (NumberFormatException e) {
      // Long.parseLong's own exception message embeds the raw, unsanitized input — rethrow with the same
      // sanitized-message convention used everywhere else in this class before it reaches the resolveLive log.
      throw new IllegalStateException(format("GitLab repository id is not numeric: '%s'", sanitizeForLog(almRepo)));
    }
    Project project = gitlabApplicationClient.getProject(url, pat, gitlabProjectId);
    String repoUrl = requireNonNull(project.getWebUrl(), format("GitLab returned no web URL for project '%s'", sanitizeForLog(almRepo)));
    return new LiveResolution(repoUrl, String.valueOf(project.getId()));
  }

  private LiveResolution resolveAzure(AlmSettingDto almSetting, ProjectAlmSettingDto projectAlmSetting) {
    String pat = requireNonNull(almSetting.getDecryptedPersonalAccessToken(encryption), PAT_CANNOT_BE_NULL);
    String url = requireNonNull(almSetting.getUrl(), URL_CANNOT_BE_NULL);
    String almSlug = requireNonNull(projectAlmSetting.getAlmSlug(), ALM_SLUG_CANNOT_BE_NULL);
    String almRepo = requireNonNull(projectAlmSetting.getAlmRepo(), ALM_REPO_CANNOT_BE_NULL);
    GsonAzureRepo repository = azureDevOpsHttpClient.getRepo(url, pat, almSlug, almRepo);
    String safeAlmRepo = sanitizeForLog(almRepo);
    String repoUrl = requireNonNull(repository.getWebUrl(), format("Azure DevOps returned no web URL for repository '%s'", safeAlmRepo));
    String repoId = requireNonNull(repository.getId(), format("Azure DevOps returned no id for repository '%s'", safeAlmRepo));
    return new LiveResolution(repoUrl, repoId);
  }

  private LiveResolution resolveBitbucketServer(AlmSettingDto almSetting, ProjectAlmSettingDto projectAlmSetting) {
    String pat = requireNonNull(almSetting.getDecryptedPersonalAccessToken(encryption), PAT_CANNOT_BE_NULL);
    String serverUrl = requireNonNull(almSetting.getUrl(), URL_CANNOT_BE_NULL);
    String almRepo = requireNonNull(projectAlmSetting.getAlmRepo(), ALM_REPO_CANNOT_BE_NULL);
    String almSlug = requireNonNull(projectAlmSetting.getAlmSlug(), ALM_SLUG_CANNOT_BE_NULL);
    org.sonar.alm.client.bitbucketserver.Repository repository = bitbucketServerRestClient.getRepo(serverUrl, pat, almRepo, almSlug);
    String url = repository.getSelfHref();
    if (url == null) {
      throw new IllegalStateException(format("No self link found for Bitbucket Server repository '%s'", sanitizeForLog(almSlug)));
    }
    return new LiveResolution(url, String.valueOf(repository.getId()));
  }

  private LiveResolution resolveBitbucketCloud(AlmSettingDto almSetting, ProjectAlmSettingDto projectAlmSetting, Map<String, String> tokenCache) {
    String clientId = requireNonNull(almSetting.getClientId(), CLIENT_ID_CANNOT_BE_NULL);
    String clientSecret = requireNonNull(almSetting.getDecryptedClientSecret(encryption), CLIENT_SECRET_CANNOT_BE_NULL);
    String workspace = requireNonNull(almSetting.getAppId(), WORKSPACE_CANNOT_BE_NULL);
    String almRepo = requireNonNull(projectAlmSetting.getAlmRepo(), ALM_REPO_CANNOT_BE_NULL);
    // The OAuth token is workspace-wide, not per-repository: reuse it across every binding of this ALM setting
    // resolved within the same request instead of minting a fresh one per binding.
    String accessToken = tokenCache.computeIfAbsent(almSetting.getUuid(), uuid -> bitbucketCloudRestClient.createAccessToken(clientId, clientSecret));
    org.sonar.alm.client.bitbucket.bitbucketcloud.Repository repository = bitbucketCloudRestClient.getRepoWithAccessToken(accessToken, workspace, almRepo);
    String url = repository.getHtmlHref();
    if (url == null) {
      throw new IllegalStateException(format("No html link found for Bitbucket Cloud repository '%s'", sanitizeForLog(almRepo)));
    }
    String repoId = repository.getUuid();
    if (repoId == null) {
      throw new IllegalStateException(format("No uuid found for Bitbucket Cloud repository '%s'", sanitizeForLog(almRepo)));
    }
    return new LiveResolution(url, repoId);
  }

  /**
   * GitHub's installation-token "repositories" scoping parameter expects the bare repository name
   * (no {@code owner/} prefix), unlike {@code almRepo} which is stored as {@code owner/repo}.
   */
  private static String bareRepositoryName(String almRepo) {
    int lastSlash = almRepo.lastIndexOf('/');
    return lastSlash < 0 ? almRepo : almRepo.substring(lastSlash + 1);
  }

  /**
   * Strips CR/LF from ALM-supplied identifiers (repo/project keys, slugs) before they're embedded in an exception
   * message that ends up in a log line — otherwise a crafted value could forge extra log entries (CWE-117). Only
   * used for messages; the real, unsanitized value is always what's sent to the ALM REST clients.
   */
  private static String sanitizeForLog(@Nullable String value) {
    return value == null ? "null" : CRLF_PATTERN.matcher(value).replaceAll("_");
  }

  private static String toDevOpsPlatform(ALM alm) {
    return switch (alm) {
      case GITHUB -> "github";
      case GITLAB -> "gitlab";
      case AZURE_DEVOPS -> "azure_devops";
      case BITBUCKET_CLOUD -> "bitbucketcloud";
      case BITBUCKET -> "bitbucket";
    };
  }

  private static String toSlug(ALM alm, ProjectAlmSettingDto projectAlmSetting) {
    return switch (alm) {
      case GITHUB, BITBUCKET_CLOUD -> projectAlmSetting.getAlmRepo();
      case GITLAB, BITBUCKET -> projectAlmSetting.getAlmSlug();
      case AZURE_DEVOPS -> "";
    };
  }

  private static ALM fromDevOpsPlatform(String devOpsPlatform) {
    return switch (devOpsPlatform) {
      case "github" -> ALM.GITHUB;
      case "gitlab" -> ALM.GITLAB;
      case "azure_devops" -> ALM.AZURE_DEVOPS;
      case "bitbucketcloud" -> ALM.BITBUCKET_CLOUD;
      case "bitbucket" -> ALM.BITBUCKET;
      default -> null;
    };
  }

  private record LiveResolution(String url, String repoId) {
  }

}
