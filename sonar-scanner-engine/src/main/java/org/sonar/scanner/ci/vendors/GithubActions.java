/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.ci.vendors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.System2;
import org.sonar.scanner.ci.CiConfiguration;
import org.sonar.scanner.ci.CiConfigurationImpl;
import org.sonar.scanner.ci.CiVendor;
import org.sonar.scanner.ci.DevOpsPlatformInfo;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Support of https://github.com/features/actions
 * <p>
 * Environment variables: https://developer.github.com/actions/creating-github-actions/accessing-the-runtime-environment/#environment-variables
 */
public class GithubActions implements CiVendor {

  private static final Logger LOG = LoggerFactory.getLogger(GithubActions.class);

  private static final String PROPERTY_COMMIT = "GITHUB_SHA";
  public static final String GITHUB_REPOSITORY_ENV_VAR = "GITHUB_REPOSITORY";
  public static final String GITHUB_API_URL_ENV_VAR = "GITHUB_API_URL";

  private final System2 system;

  public GithubActions(System2 system) {
    this.system = system;
  }

  @Override
  public String getName() {
    return "Github Actions";
  }

  @Override
  public boolean isDetected() {
    return StringUtils.isNotBlank(system.envVariable("GITHUB_ACTION"));
  }

  @Override
  public CiConfiguration loadConfiguration() {
    String revision = system.envVariable(PROPERTY_COMMIT);
    if (isEmpty(revision)) {
      LOG.warn("Missing environment variable " + PROPERTY_COMMIT);
    }

    String githubRepository = system.envVariable(GITHUB_REPOSITORY_ENV_VAR);
    String githubApiUrl = system.envVariable(GITHUB_API_URL_ENV_VAR);
    if (isEmpty(githubRepository) || isEmpty(githubApiUrl)) {
      LOG.warn("Missing or empty environment variables: {}, and/or {}", GITHUB_API_URL_ENV_VAR, GITHUB_REPOSITORY_ENV_VAR);
      return new CiConfigurationImpl(revision, getName());
    }
    return new CiConfigurationImpl(revision, getName(), new DevOpsPlatformInfo(githubApiUrl, githubRepository));

  }
}
