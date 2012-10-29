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
package org.sonar.plugins.jacoco;

import org.apache.commons.lang.StringUtils;
import org.jacoco.core.runtime.AgentOptions;
import org.sonar.api.BatchExtension;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.plugins.java.api.JavaSettings;

@Properties({
    @Property(
        key = JacocoConfiguration.REPORT_PATH_PROPERTY,
        name = "File with execution data",
        defaultValue = JacocoConfiguration.REPORT_PATH_DEFAULT_VALUE,
        description = "Path (absolute or relative) to the file with execution data.",
        global = false,
        module = true,
        project = true
    ),
    @Property(
        key = JacocoConfiguration.INCLUDES_PROPERTY,
        name = "Includes",
        description = "A list of class names that should be included in execution analysis." +
            " The list entries are separated by a colon (:) and may use wildcard characters (* and ?)." +
            " Except for performance optimization or technical corner cases this option is normally not required.",
        global = true,
        project = true,
        module = true
    ),
    @Property(
        key = JacocoConfiguration.EXCLUDES_PROPERTY,
        name = "Excludes",
        defaultValue = JacocoConfiguration.EXCLUDES_DEFAULT_VALUE,
        description = "A list of class names that should be excluded from execution analysis." +
            " The list entries are separated by a colon (:) and may use wildcard characters (* and ?)." +
            " Except for performance optimization or technical corner cases this option is normally not required.",
        global = true,
        project = true,
        module = true
    ),
    @Property(
        key = JacocoConfiguration.EXCLCLASSLOADER_PROPERTY,
        name = "Excluded class loaders",
        description = "A list of class loader names that should be excluded from execution analysis." +
            " The list entries are separated by a colon (:) and may use wildcard characters (* and ?)." +
            " This option might be required in case of special frameworks that conflict with JaCoCo code" +
            " instrumentation, in particular class loaders that do not have access to the Java runtime classes.",
        global = true,
        project = true,
        module = true
    ),
    @Property(
        key = JacocoConfiguration.IT_REPORT_PATH_PROPERTY,
        name = "File with execution data for integration tests",
        defaultValue = JacocoConfiguration.IT_REPORT_PATH_DEFAULT_VALUE,
        description = "Path (absolute or relative) to the file with execution data.",
        global = false,
        module = true,
        project = true
    ),
    @Property(
        key = JacocoConfiguration.ANT_TARGETS_PROPERTY,
        name = "Ant targets",
        defaultValue = JacocoConfiguration.ANT_TARGETS_DEFAULT_VALUE,
        description = "Comma separated list of Ant targets for execution of tests.",
        global = true,
        module = true,
        project = true
    )})
public class JacocoConfiguration implements BatchExtension {

  public static final String REPORT_PATH_PROPERTY = "sonar.jacoco.reportPath";
  public static final String REPORT_PATH_DEFAULT_VALUE = "target/jacoco.exec";
  public static final String IT_REPORT_PATH_PROPERTY = "sonar.jacoco.itReportPath";
  public static final String IT_REPORT_PATH_DEFAULT_VALUE = "";
  public static final String INCLUDES_PROPERTY = "sonar.jacoco.includes";
  public static final String EXCLUDES_PROPERTY = "sonar.jacoco.excludes";

  /**
   * Hibernate uses Javassist to modify entity classes and without exclusion of such classes from JaCoCo exception might be thrown:
   * <pre>
   * Javassist Enhancement failed: org.sonar.api.profiles.Alert
   * java.lang.VerifyError: (class: org/sonar/api/profiles/Alert_$$_javassist_3, method: <clinit> signature: ()V) Illegal local variable number
   * </pre>
   */
  public static final String EXCLUDES_DEFAULT_VALUE = "*_javassist_*";
  public static final String EXCLCLASSLOADER_PROPERTY = "sonar.jacoco.exclclassloader";
  public static final String ANT_TARGETS_PROPERTY = "sonar.jacoco.antTargets";
  public static final String ANT_TARGETS_DEFAULT_VALUE = "";

  private Settings settings;
  private JavaSettings javaSettings;
  private JaCoCoAgentDownloader downloader;

  public JacocoConfiguration(Settings settings, JaCoCoAgentDownloader downloader, JavaSettings javaSettings) {
    this.settings = settings;
    this.downloader = downloader;
    this.javaSettings = javaSettings;
  }

  public boolean isEnabled(Project project) {
    return Java.KEY.equals(project.getLanguageKey()) &&
      project.getAnalysisType().isDynamic(true) &&
      JaCoCoUtils.PLUGIN_KEY.equals(javaSettings.getEnabledCoveragePlugin());
  }

  public String getReportPath() {
    return settings.getString(REPORT_PATH_PROPERTY);
  }

  public String getItReportPath() {
    return settings.getString(IT_REPORT_PATH_PROPERTY);
  }

  public String getJvmArgument() {
    AgentOptions options = new AgentOptions();
    options.setDestfile(getReportPath());
    String includes = settings.getString(INCLUDES_PROPERTY);
    if (StringUtils.isNotBlank(includes)) {
      options.setIncludes(includes);
    }
    String excludes = settings.getString(EXCLUDES_PROPERTY);
    if (StringUtils.isNotBlank(excludes)) {
      options.setExcludes(excludes);
    }
    String exclclassloader = settings.getString(EXCLCLASSLOADER_PROPERTY);
    if (StringUtils.isNotBlank(exclclassloader)) {
      options.setExclClassloader(exclclassloader);
    }
    return options.getVMArgument(downloader.getAgentJarFile());
  }

  public String[] getAntTargets() {
    return settings.getStringArray(ANT_TARGETS_PROPERTY);
  }

  public String getExcludes() {
    return settings.getString(EXCLUDES_PROPERTY);
  }

}
