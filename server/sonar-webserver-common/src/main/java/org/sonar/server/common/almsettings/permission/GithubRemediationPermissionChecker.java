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

import com.google.common.net.UrlEscapers;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.alm.client.github.GithubApplicationClientImpl;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.api.server.ServerSide;
import org.sonar.auth.github.GithubAppConfiguration;
import org.sonar.auth.github.GithubAppInstallationDetails;
import org.sonar.auth.github.GithubAppPermissions;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonarqube.ws.client.HttpException;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

/**
 * Checks a GitHub configuration against the permissions the Remediation Agent needs, at both levels that can block it
 * (SONAR-32166).
 *
 * <p>The app-level check reads {@code /app}, which reports the permissions the app is <em>configured</em> to request.
 * That alone is not enough: when an administrator adds a permission to an existing app, every installation keeps
 * running on the permissions it approved earlier until its owner approves the change. So an app can report
 * {@code contents: write} while the installation the Remediation Agent would actually push through still grants
 * {@code contents: read}. This class therefore also reads the permissions granted to each installation.
 *
 * <p>The two findings are reported separately because they need different people to act, and the second is
 * deliberately the narrower of the two: an installation is only ever reported for permissions the app actually
 * requests. A permission the app was never configured to request is missing from every installation by construction,
 * and no installation owner can approve it, so it is reported once against the app rather than as a deficit on every
 * installation in the list. The instance administrator fixes that first, on the app's settings page.
 */
@ServerSide
public class GithubRemediationPermissionChecker {

  private static final Logger LOG = LoggerFactory.getLogger(GithubRemediationPermissionChecker.class);

  private static final Comparator<AffectedInstallation> STABLE_ORDER = Comparator
    .comparing(AffectedInstallation::installationOwner, String.CASE_INSENSITIVE_ORDER)
    .thenComparing(AffectedInstallation::installationId);

  private final GithubGlobalSettingsValidator githubGlobalSettingsValidator;
  private final GithubApplicationClient githubApplicationClient;

  public GithubRemediationPermissionChecker(GithubGlobalSettingsValidator githubGlobalSettingsValidator, GithubApplicationClientImpl githubApplicationClient) {
    this.githubGlobalSettingsValidator = githubGlobalSettingsValidator;
    this.githubApplicationClient = githubApplicationClient;
  }

  /**
   * Instance-wide verdict for one configuration: the app's own permissions, plus every installation that has not
   * approved what the Remediation Agent requires. Every installation of the app is scanned, whoever owns it.
   */
  public DopPermissionCheck checkConnection(AlmSettingDto almSetting) {
    AppLevelCheck appLevelCheck = checkApp(almSetting);
    if (appLevelCheck.unusable()) {
      return DopPermissionCheck.githubInstallationCheckFailed(List.of());
    }

    List<GithubAppInstallationDetails> installations;
    try {
      installations = githubApplicationClient.getAllAppInstallations(appLevelCheck.configuration());
    } catch (Exception e) {
      LOG.debug("Could not list the GitHub App installations of configuration '{}'", almSetting.getKey(), e);
      return DopPermissionCheck.githubInstallationCheckFailed(appLevelCheck.deficits());
    }

    List<AffectedInstallation> affectedInstallations = installations.stream()
      .map(installation -> toAffectedInstallation(appLevelCheck, installation))
      .filter(Objects::nonNull)
      .sorted(STABLE_ORDER)
      .toList();
    return DopPermissionCheck.githubComplete(appLevelCheck.deficits(), installations.size(), affectedInstallations);
  }

  /**
   * Verdict for one project, from the installation covering that project's own repository — which may well differ from
   * the verdict of any other project on the same configuration, and is unaffected by what other installations have
   * approved.
   *
   * @param repositorySlug {@code owner/repository} the project is bound to, or {@code null} when it has none
   */
  public DopPermissionCheck checkProject(AlmSettingDto almSetting, @Nullable String repositorySlug) {
    AppLevelCheck appLevelCheck = checkApp(almSetting);
    if (appLevelCheck.unusable()) {
      return DopPermissionCheck.githubProject(PermissionCheckStatus.CHECK_FAILED, List.of(), InstallationCheckStatus.FAILED);
    }
    if (StringUtils.isBlank(repositorySlug)) {
      // Bound to the configuration but to no repository: there is no installation to look up, so the app's own
      // configuration is all that can be judged.
      return DopPermissionCheck.githubProject(statusOf(appLevelCheck.deficits(), List.of()), appLevelCheck.deficits(), InstallationCheckStatus.NOT_RUN);
    }

    List<PermissionDeficit> installationDeficits;
    try {
      GithubAppInstallationDetails installation = githubApplicationClient.getRepositoryInstallation(appLevelCheck.configuration(), repositorySlug);
      installationDeficits = appLevelCheck.findInstallationDeficits(installation);
    } catch (HttpException e) {
      if (e.code() == HTTP_NOT_FOUND) {
        // A definite answer, not a failed check: the app is simply not installed on that repository. The Remediation
        // Agent cannot act, but the fix is to install the app, not to retry the verification.
        LOG.debug("The GitHub App of configuration '{}' is not installed on the repository bound to this project", almSetting.getKey());
        return DopPermissionCheck.githubProject(PermissionCheckStatus.INSUFFICIENT, appLevelCheck.deficits(), InstallationCheckStatus.NOT_INSTALLED);
      }
      LOG.debug("Could not read the GitHub App installation covering the repository bound to configuration '{}'", almSetting.getKey(), e);
      return DopPermissionCheck.githubProject(PermissionCheckStatus.CHECK_FAILED, appLevelCheck.deficits(), InstallationCheckStatus.FAILED);
    } catch (Exception e) {
      LOG.debug("Could not read the GitHub App installation covering the repository bound to configuration '{}'", almSetting.getKey(), e);
      return DopPermissionCheck.githubProject(PermissionCheckStatus.CHECK_FAILED, appLevelCheck.deficits(), InstallationCheckStatus.FAILED);
    }
    return DopPermissionCheck.githubProject(statusOf(appLevelCheck.deficits(), installationDeficits), appLevelCheck.deficits(), InstallationCheckStatus.COMPLETE);
  }

  private AppLevelCheck checkApp(AlmSettingDto almSetting) {
    try {
      GithubAppConfiguration configuration = githubGlobalSettingsValidator.validateApiEndpoint(almSetting);
      return AppLevelCheck.of(configuration, githubApplicationClient.getAppPermissions(configuration));
    } catch (Exception e) {
      LOG.debug("Could not check the GitHub App configuration of '{}'", almSetting.getKey(), e);
      return AppLevelCheck.configurationUnavailable();
    }
  }

  private static PermissionCheckStatus statusOf(List<PermissionDeficit> appDeficits, List<PermissionDeficit> installationDeficits) {
    return appDeficits.isEmpty() && installationDeficits.isEmpty() ? PermissionCheckStatus.SUFFICIENT : PermissionCheckStatus.INSUFFICIENT;
  }

  @Nullable
  private static AffectedInstallation toAffectedInstallation(AppLevelCheck appLevelCheck, GithubAppInstallationDetails installation) {
    List<PermissionDeficit> deficits = appLevelCheck.findInstallationDeficits(installation);
    if (deficits.isEmpty()) {
      // Includes suspended installations that granted everything: a suspension is not a permission to approve, and
      // listing it here would send an administrator to a settings page with nothing to tick.
      return null;
    }
    return new AffectedInstallation(
      Long.toString(installation.installationId()),
      installation.accountLogin(),
      settingsUrl(appLevelCheck.configuration(), installation),
      deficits);
  }

  /**
   * The page where the installation's owner approves the missing permissions, built on the host this configuration is
   * bound to.
   *
   * <p>GitHub reports a URL of its own as {@code html_url}, and this deliberately does not use it. GitHub's REST
   * reference documents the field as a bare {@code uri} with no example, so its shape cannot be confirmed; and this
   * repository's own GitHub Enterprise fixtures carry a github.com {@code html_url} alongside enterprise-host URLs
   * everywhere else in the same payload. A link to the wrong host, or to the personal settings page for an
   * installation an organization owns, sends an administrator somewhere they cannot approve anything — worse than no
   * link at all. The path is stable and short enough to build correctly for both owner kinds.
   */
  private static String settingsUrl(GithubAppConfiguration configuration, GithubAppInstallationDetails installation) {
    String baseUrl = GithubApplicationClientImpl.convertApiUrlToBaseUrl(configuration.getApiEndpoint());
    // An installation an organization owns is approved from that organization's settings, not from the personal
    // settings page — the same /organizations/{login}/settings/... shape the GitHub App creation URL already uses.
    String ownerPath = installation.isOrganization() ? ("/organizations/" + escapePathSegment(installation.accountLogin())) : "";
    return baseUrl + ownerPath + "/settings/installations/" + installation.installationId();
  }

  private static String escapePathSegment(String segment) {
    return UrlEscapers.urlPathSegmentEscaper().escape(segment);
  }

  /**
   * Outcome of the app-level part of a check, and the yardstick the installations are then measured against.
   *
   * <p>{@link #unusable()} means the configuration itself could not be used — bad credentials, unreachable host — which
   * stops the check: there is nothing to compare installations against.
   *
   * @param requestedPermissions the required permissions the app does request at a sufficient level. Installations are
   *                             compared against these rather than the full requirement set, so that an app-level gap
   *                             is reported once against the app instead of once against every installation.
   */
  private record AppLevelCheck(@Nullable GithubAppConfiguration configuration, List<PermissionDeficit> deficits, Map<String, String> requestedPermissions) {

    static AppLevelCheck of(GithubAppConfiguration configuration, Map<String, String> grantedAppPermissions) {
      Map<String, String> required = GithubAppPermissions.TOKEN_MINTING_PERMISSIONS;
      List<PermissionDeficit> deficits = GithubPermissionComparison.findDeficits(required, grantedAppPermissions);
      Map<String, String> requested = new LinkedHashMap<>(required);
      deficits.forEach(deficit -> requested.remove(deficit.permission()));
      return new AppLevelCheck(configuration, deficits, Map.copyOf(requested));
    }

    static AppLevelCheck configurationUnavailable() {
      return new AppLevelCheck(null, List.of(), Map.of());
    }

    boolean unusable() {
      return configuration == null;
    }

    List<PermissionDeficit> findInstallationDeficits(GithubAppInstallationDetails installation) {
      return GithubPermissionComparison.findDeficits(requestedPermissions, installation.permissions());
    }
  }
}
