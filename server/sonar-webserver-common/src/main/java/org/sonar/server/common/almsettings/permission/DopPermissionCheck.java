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
import javax.annotation.Nullable;

/**
 * Structured result of a DevOps Platform permission check.
 *
 * <p>Beyond the overall {@link #status()}, a GitHub check also reports <em>where</em> the deficit is, because the two
 * places call for different actions (SONAR-32166): {@link #appMissingPermissions()} lists what the GitHub App itself
 * was never configured to request — which an instance administrator fixes on the app's settings page — while
 * {@link #affectedInstallations()} lists the installations that have not approved permissions the app does request,
 * which only each installation's owner can approve. An app-level deficit has to be fixed first, and appears in
 * {@link #appMissingPermissions()} alone: an installation cannot approve a permission the app never asked for, so
 * repeating it against every installation would only produce findings nobody can act on.
 *
 * <p>The installation counts and {@link #affectedInstallations()} are meaningful only when
 * {@link #installationCheckStatus()} is {@link InstallationCheckStatus#COMPLETE}; otherwise they are {@code null} and
 * empty, rather than an empty result that would read as "every installation has approved".
 *
 * <p>{@link #totalInstallationCount()} exists so that an app nobody has installed is distinguishable from an app whose
 * installations have all approved: both report zero affected installations, and only the total tells them apart.
 *
 * @param appMissingPermissions      deficits in the app's own configuration; empty when it is correctly configured
 * @param installationCheckStatus    whether the per-installation scan produced a trustworthy answer
 * @param totalInstallationCount     how many installations the app has at all, or {@code null} without a complete scan
 * @param affectedInstallationCount  exact number of affected installations, or {@code null} without a complete scan
 * @param affectedInstallations      every affected installation, in a stable order
 */
public record DopPermissionCheck(
  PermissionCheckStatus status,
  List<PermissionDeficit> appMissingPermissions,
  InstallationCheckStatus installationCheckStatus,
  @Nullable Integer totalInstallationCount,
  @Nullable Integer affectedInstallationCount,
  List<AffectedInstallation> affectedInstallations) {

  public DopPermissionCheck {
    appMissingPermissions = List.copyOf(appMissingPermissions);
    affectedInstallations = List.copyOf(affectedInstallations);
  }

  public static DopPermissionCheck sufficient() {
    return withoutDetails(PermissionCheckStatus.SUFFICIENT);
  }

  public static DopPermissionCheck insufficient() {
    return withoutDetails(PermissionCheckStatus.INSUFFICIENT);
  }

  public static DopPermissionCheck unknown() {
    return withoutDetails(PermissionCheckStatus.UNKNOWN);
  }

  public static DopPermissionCheck checkFailed() {
    return withoutDetails(PermissionCheckStatus.CHECK_FAILED);
  }

  public static DopPermissionCheck unsupportedTokenType() {
    return withoutDetails(PermissionCheckStatus.UNSUPPORTED_TOKEN_TYPE);
  }

  /**
   * A verdict carrying no GitHub installation detail: the GitLab and Azure DevOps checks, which have no equivalent
   * notion, and the GitHub failures that happen before any installation is read.
   */
  private static DopPermissionCheck withoutDetails(PermissionCheckStatus status) {
    return new DopPermissionCheck(status, List.of(), InstallationCheckStatus.NOT_RUN, null, null, List.of());
  }

  /**
   * A GitHub check whose installation scan read every page: both counts and the list are exhaustive.
   */
  public static DopPermissionCheck githubComplete(List<PermissionDeficit> appMissingPermissions, int totalInstallationCount,
    List<AffectedInstallation> affectedInstallations) {
    PermissionCheckStatus status = appMissingPermissions.isEmpty() && affectedInstallations.isEmpty()
      ? PermissionCheckStatus.SUFFICIENT
      : PermissionCheckStatus.INSUFFICIENT;
    return new DopPermissionCheck(status, appMissingPermissions, InstallationCheckStatus.COMPLETE, totalInstallationCount, affectedInstallations.size(),
      affectedInstallations);
  }

  /**
   * A GitHub check whose installation scan could not be completed. Partial findings are discarded: a list built from
   * the pages that happened to load cannot support either "these are the installations to fix" or "there are none".
   * Whatever was learned about the app's own configuration is still reported, since that part did succeed.
   */
  public static DopPermissionCheck githubInstallationCheckFailed(List<PermissionDeficit> appMissingPermissions) {
    return new DopPermissionCheck(PermissionCheckStatus.CHECK_FAILED, appMissingPermissions, InstallationCheckStatus.FAILED, null, null, List.of());
  }

  /**
   * A GitHub check scoped to one project: the verdict covers that project's own installation only, so it carries no
   * installation list and no counts. {@code installationCheckStatus} distinguishes a project whose installation was read
   * ({@link InstallationCheckStatus#COMPLETE}) from one where the app is not installed on the repository at all
   * ({@link InstallationCheckStatus#NOT_INSTALLED}), one that could not be read
   * ({@link InstallationCheckStatus#FAILED}), and one never looked up because the project has no bound repository
   * ({@link InstallationCheckStatus#NOT_RUN}).
   */
  public static DopPermissionCheck githubProject(PermissionCheckStatus status, List<PermissionDeficit> appMissingPermissions,
    InstallationCheckStatus installationCheckStatus) {
    return new DopPermissionCheck(status, appMissingPermissions, installationCheckStatus, null, null, List.of());
  }
}
