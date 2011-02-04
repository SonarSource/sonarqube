/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.sensors;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectLink;

public class ProjectLinksSensor implements Sensor {

  public static final String KEY_HOME = "homepage";
  public static final String KEY_CONTINUOUS_INTEGRATION = "ci";
  public static final String KEY_ISSUE_TRACKER = "issue";
  public static final String KEY_SCM = "scm";
  public static final String KEY_SCM_DEVELOPER_CONNECTION = "scm_dev";

  private MavenProject pom;

  public ProjectLinksSensor(MavenProject pom) {
    this.pom = pom;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void analyse(Project project, SensorContext context) {
    updateLink(context, KEY_HOME, "Home", pom.getUrl());

    Scm scm = pom.getScm();
    if (scm == null) {
      scm = new Scm();
    }
    updateLink(context, KEY_SCM, "Sources", scm.getUrl());
    updateLink(context, KEY_SCM_DEVELOPER_CONNECTION, "Developer connection", scm.getDeveloperConnection());

    CiManagement ci = pom.getCiManagement();
    if (ci == null) {
      ci = new CiManagement();
    }
    updateLink(context, KEY_CONTINUOUS_INTEGRATION, "Continuous integration", ci.getUrl());

    IssueManagement issues = pom.getIssueManagement();
    if (issues == null) {
      issues = new IssueManagement();
    }
    updateLink(context, KEY_ISSUE_TRACKER, "Issues", issues.getUrl());
  }

  private void updateLink(SensorContext context, String key, String name, String url) {
    if (StringUtils.isBlank(url)) {
      context.deleteLink(key);
    } else {
      context.saveLink(new ProjectLink(key, name, url));
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
