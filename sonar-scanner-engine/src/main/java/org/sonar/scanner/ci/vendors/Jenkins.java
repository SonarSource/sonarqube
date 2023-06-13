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

import java.nio.file.Path;
import java.util.Optional;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.utils.System2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.scanner.ci.CiConfiguration;
import org.sonar.scanner.ci.CiConfigurationImpl;
import org.sonar.scanner.ci.CiVendor;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class Jenkins implements CiVendor {
  private static final Logger LOG = LoggerFactory.getLogger(Jenkins.class);
  private final System2 system;
  private final DefaultInputProject inputProject;

  public Jenkins(System2 system, DefaultInputProject inputProject) {
    this.system = system;
    this.inputProject = inputProject;
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
    if (StringUtils.isNotBlank(revision)) {
      return new CiConfigurationImpl(revision, getName());
    }

    revision = system.envVariable("GIT_COMMIT");

    if (StringUtils.isNotBlank(revision)) {
      if (StringUtils.isNotBlank(system.envVariable("CHANGE_ID"))) {
        String jenkinsGitPrSha1 = getJenkinsGitPrSha1();
        if (StringUtils.isNotBlank(jenkinsGitPrSha1)) {
          return new CiConfigurationImpl(jenkinsGitPrSha1, getName());
        }
      }
      return new CiConfigurationImpl(revision, getName());
    }

    revision = system.envVariable("SVN_COMMIT");
    return new CiConfigurationImpl(revision, getName());
  }

  private String getJenkinsGitPrSha1() {
    String gitBranch = system.envVariable("GIT_BRANCH");
    if (StringUtils.isBlank(gitBranch)) {
      return null;
    }

    Path baseDir = inputProject.getBaseDir();

    RepositoryBuilder builder = new RepositoryBuilder()
      .findGitDir(baseDir.toFile())
      .setMustExist(true);

    if (builder.getGitDir() == null) {
      return null;
    }

    String refName = "refs/remotes/origin/" + gitBranch;
    try (Repository repo = builder.build()) {
      return Optional.ofNullable(repo.exactRef(refName))
        .map(Ref::getObjectId)
        .map(ObjectId::getName)
        .orElse(null);
    } catch (Exception e) {
      LOG.debug("Couldn't find git sha1 in '{}': {}", refName, e.getMessage());
    }
    return null;
  }

}
