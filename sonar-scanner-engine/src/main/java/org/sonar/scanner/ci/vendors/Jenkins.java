/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.api.utils.System2;
import org.sonar.scanner.ci.CiConfiguration;
import org.sonar.scanner.ci.CiConfigurationImpl;
import org.sonar.scanner.ci.CiVendor;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class Jenkins implements CiVendor {
  private final System2 system;

  public Jenkins(System2 system) {
    this.system = system;
  }

  @Override
  public String getName() {
    return "Jenkins";
  }

  @Override
  public boolean isDetected() {
    // https://wiki.jenkins-ci.org/display/JENKINS/Building+a+software+project
    // JENKINS_URL is not enough to identify Jenkins. It can be easily used on a non-Jenkins job.
    return isNotBlank(system.envVariable("JENKINS_URL")) && isNotBlank(system.envVariable("EXECUTOR_NUMBER"));
  }

  @Override
  public CiConfiguration loadConfiguration() {
    // https://wiki.jenkins-ci.org/display/JENKINS/GitHub+pull+request+builder+plugin#GitHubpullrequestbuilderplugin-EnvironmentVariables
    // https://wiki.jenkins-ci.org/display/JENKINS/Building+a+software+project
    String revision = system.envVariable("ghprbActualCommit");
    if (StringUtils.isBlank(revision)) {
      revision = system.envVariable("GIT_COMMIT");
      if (StringUtils.isBlank(revision)) {
        revision = system.envVariable("SVN_COMMIT");
      }
    }
    return new CiConfigurationImpl(revision);
  }
}
