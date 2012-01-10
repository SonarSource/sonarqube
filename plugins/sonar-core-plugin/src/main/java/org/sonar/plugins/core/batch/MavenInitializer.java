/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.plugins.core.batch;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.project.MavenProject;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.Initializer;
import org.sonar.api.batch.SupportedEnvironment;
import org.sonar.api.batch.maven.MavenUtils;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.Logs;
import org.sonar.java.api.JavaUtils;

@SupportedEnvironment("maven")
public class MavenInitializer extends Initializer {

  @Override
  public void execute(Project project) {
    MavenProject pom = project.getPom();
    Configuration conf = project.getConfiguration();
    /*
     * See http://jira.codehaus.org/browse/SONAR-2148
     * Get Java source and target versions from maven-compiler-plugin.
     */
    if (StringUtils.isBlank(conf.getString(JavaUtils.JAVA_SOURCE_PROPERTY))) {
      String version = MavenUtils.getJavaSourceVersion(pom);
      conf.setProperty(JavaUtils.JAVA_SOURCE_PROPERTY, version);
      Logs.INFO.info("Java source version: {}", JavaUtils.getSourceVersion(project));
    }
    if (StringUtils.isBlank(conf.getString(JavaUtils.JAVA_TARGET_PROPERTY))) {
      String version = MavenUtils.getJavaVersion(pom);
      conf.setProperty(JavaUtils.JAVA_TARGET_PROPERTY, version);
      Logs.INFO.info("Java target version: {}", JavaUtils.getTargetVersion(project));
    }
    /*
     * See http://jira.codehaus.org/browse/SONAR-2151
     * Get source encoding from POM
     */
    if (StringUtils.isBlank(conf.getString(CoreProperties.ENCODING_PROPERTY))) {
      String encoding = MavenUtils.getSourceEncoding(pom);
      conf.setProperty(CoreProperties.ENCODING_PROPERTY, encoding);
      Logs.INFO.info("Source encoding: {}", encoding);
    }
  }

}
