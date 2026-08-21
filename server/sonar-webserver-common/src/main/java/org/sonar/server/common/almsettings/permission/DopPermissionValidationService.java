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
package org.sonar.server.common.almsettings.permission;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;
import org.sonar.alm.client.azure.AzureDevOpsValidator;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.alm.client.gitlab.GitlabGlobalSettingsValidator;
import org.sonar.alm.client.gitlab.GitlabServerException;
import org.sonar.api.server.ServerSide;
import org.sonar.auth.github.GithubAppPermissions;
import org.sonar.db.alm.setting.AlmSettingDto;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;

/**
 * Validates whether a configured DevOps Platform instance grants the write permissions the SonarQube Remediation Agent
 * needs to clone a repository, push a branch and open a pull/merge request. It reuses the existing per-platform
 * validators — the same checks that run when minting an SCM token for the orchestrator — but returns a structured
 * {@link DopPermissionCheck} instead of throwing, so the outcome can be surfaced in the UI (SONAR-31626).
 *
 * <p>Only GitHub, GitLab and Azure DevOps are supported; Bitbucket is out of scope. The validation runs live against the
 * platform on every call — caching is layered on separately (SONAR-31641).
 */
@ServerSide
public class DopPermissionValidationService {

  private static final String INSUFFICIENT_SCOPE_MARKER = "insufficient scope";

  private final GithubGlobalSettingsValidator githubGlobalSettingsValidator;
  private final GitlabGlobalSettingsValidator gitlabGlobalSettingsValidator;
  private final AzureDevOpsValidator azureDevOpsValidator;

  public DopPermissionValidationService(GithubGlobalSettingsValidator githubGlobalSettingsValidator,
    GitlabGlobalSettingsValidator gitlabGlobalSettingsValidator, AzureDevOpsValidator azureDevOpsValidator) {
    this.githubGlobalSettingsValidator = githubGlobalSettingsValidator;
    this.gitlabGlobalSettingsValidator = gitlabGlobalSettingsValidator;
    this.azureDevOpsValidator = azureDevOpsValidator;
  }

  /**
   * Checks the given DevOps Platform configuration against the Remediation Agent's required write permissions.
   *
   * @throws IllegalArgumentException if the configuration's platform is not supported (Bitbucket).
   */
  public DopPermissionCheck check(AlmSettingDto almSetting) {
    return switch (almSetting.getAlm()) {
      case GITHUB -> checkGithub(almSetting);
      case GITLAB -> checkGitlab(almSetting);
      case AZURE_DEVOPS -> checkAzure(almSetting);
      case BITBUCKET, BITBUCKET_CLOUD ->
        throw new IllegalArgumentException("DevOps Platform '" + almSetting.getAlm() + "' is not supported by the Remediation Agent");
    };
  }

  /**
   * Checks several configurations in parallel and returns the results in the same order as the input. Each individual
   * check is time-bounded by the ALM client's connect/read timeouts; running them concurrently keeps the total close to
   * the slowest single platform. Uses a virtual thread per check rather than a fixed pool — these calls are blocking
   * I/O, not CPU-bound, and there are at most a handful of supported platforms per instance, so there's no pool sizing
   * to tune and no thread reuse to lose by not sharing an executor across calls. All settings must be supported
   * platforms (see {@link #check(AlmSettingDto)}).
   */
  public List<DopPermissionCheck> checkAll(List<AlmSettingDto> almSettings) {
    if (almSettings.size() <= 1) {
      return almSettings.stream().map(this::check).toList();
    }
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<CompletableFuture<DopPermissionCheck>> futures = almSettings.stream()
        .map(almSetting -> CompletableFuture.supplyAsync(() -> check(almSetting), executor))
        .toList();
      return futures.stream().map(CompletableFuture::join).toList();
    }
  }

  private DopPermissionCheck checkGithub(AlmSettingDto almSetting) {
    try {
      List<String> missingPermissions = githubGlobalSettingsValidator.findMissingPermissions(almSetting, GithubAppPermissions.TOKEN_MINTING_PERMISSIONS);
      return missingPermissions.isEmpty() ? DopPermissionCheck.sufficient() : DopPermissionCheck.insufficient();
    } catch (Exception e) {
      return DopPermissionCheck.checkFailed();
    }
  }

  private DopPermissionCheck checkGitlab(AlmSettingDto almSetting) {
    try {
      gitlabGlobalSettingsValidator.validate(almSetting);
      return DopPermissionCheck.sufficient();
    } catch (GitlabServerException e) {
      if (e.getHttpStatus() == HTTP_FORBIDDEN && hasInsufficientScope(e.getMessage())) {
        return DopPermissionCheck.insufficient();
      }
      return DopPermissionCheck.checkFailed();
    } catch (Exception e) {
      return DopPermissionCheck.checkFailed();
    }
  }

  private DopPermissionCheck checkAzure(AlmSettingDto almSetting) {
    try {
      azureDevOpsValidator.validate(almSetting);
      // Azure DevOps exposes no token-scope introspection, so a successful connectivity check cannot confirm write access.
      return DopPermissionCheck.unknown();
    } catch (Exception e) {
      return DopPermissionCheck.checkFailed();
    }
  }

  private static boolean hasInsufficientScope(@Nullable String message) {
    return message != null && message.toLowerCase(Locale.ENGLISH).contains(INSUFFICIENT_SCOPE_MARKER);
  }
}
