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

package org.sonar.scm.git;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Startable;
import org.sonar.scanner.fs.InputModuleHierarchy;
import org.sonar.scanner.repository.TelemetryCache;

public class GitRemoteUrlProvider implements Startable {
  private static final Logger LOG = LoggerFactory.getLogger(GitRemoteUrlProvider.class);
  private static final String TELEMETRY_KEY = "scanner.git_remote_url";
  private static final String UNDETECTED = "UNDETECTED";

  private final TelemetryCache telemetryCache;
  private final InputModuleHierarchy moduleHierarchy;

  public GitRemoteUrlProvider(TelemetryCache telemetryCache, InputModuleHierarchy moduleHierarchy) {
    this.telemetryCache = telemetryCache;
    this.moduleHierarchy = moduleHierarchy;
  }

  @Override
  public void start() {
    String remoteUrl = getOriginRemoteUrl();
    String sanitizedUrl = sanitizeUrl(remoteUrl);
    telemetryCache.put(TELEMETRY_KEY, sanitizedUrl);
    LOG.debug("Git remote origin URL telemetry: {}", sanitizedUrl);
  }

  @Override
  public void stop() {
    // nothing to do
  }

  private String getOriginRemoteUrl() {
    try {
      Path baseDir = moduleHierarchy.root().getBaseDir();
      try (Repository repo = JGitUtils.buildRepository(baseDir)) {
        StoredConfig config = repo.getConfig();
        String url = config.getString("remote", "origin", "url");
        return Objects.requireNonNullElse(url, UNDETECTED);
      }
    } catch (Exception e) {
      LOG.debug("Unable to get git remote origin URL", e);
      return UNDETECTED;
    }
  }

  String sanitizeUrl(String url) {
    if (UNDETECTED.equals(url)) {
      return url;
    }
    try {
      URI uri = new URI(url);
      if (uri.getUserInfo() != null) {
        // Rebuild URI without userInfo (credentials)
        return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(),
          uri.getPath(), uri.getQuery(), uri.getFragment()).toString();
      }
      return url;
    } catch (URISyntaxException e) {
      // Can't parse (e.g., SSH URL git@host:path) - return as-is
      return url;
    }
  }
}
