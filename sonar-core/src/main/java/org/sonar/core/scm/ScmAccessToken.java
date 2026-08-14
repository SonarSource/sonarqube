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
package org.sonar.core.scm;

import javax.annotation.Nullable;

/**
 * A short-lived, DOP-agnostic git credential minted for a single project's bound repository.
 *
 * @param alm       lowercase wire identifier of the DevOps Platform this credential was minted for
 *                  (e.g. {@code "github"}, {@code "gitlab"}) — deliberately a plain string, not the
 *                  {@code ALM} enum, so this sonar-core type never needs a dependency on sonar-db-dao
 * @param username  how to embed {@code secret} in a git remote URL ({@code https://username:secret@host}),
 *                  e.g. {@code "x-access-token"} for GitHub, the token's own name for GitLab
 * @param secret    the minted token
 * @param expiresAt ISO-8601 expiry timestamp of {@code secret}, or {@code null} for a platform (e.g.
 *                  Azure DevOps) whose credential has no per-request expiry to report
 */
public record ScmAccessToken(String alm, String username, String secret, @Nullable String expiresAt) {
}
