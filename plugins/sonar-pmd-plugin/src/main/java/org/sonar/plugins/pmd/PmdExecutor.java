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
package org.sonar.plugins.pmd;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Closeables;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.RuleSet;
import net.sourceforge.pmd.RuleSetFactory;
import net.sourceforge.pmd.RuleSets;
import org.sonar.api.BatchExtension;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.java.api.JavaUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

public class PmdExecutor implements BatchExtension {
  private final Project project;
  private final ProjectFileSystem projectFileSystem;
  private final RulesProfile rulesProfile;
  private final PmdProfileExporter pmdProfileExporter;
  private final PmdConfiguration pmdConfiguration;

  public PmdExecutor(Project project, ProjectFileSystem projectFileSystem, RulesProfile rulesProfile, PmdProfileExporter pmdProfileExporter, PmdConfiguration pmdConfiguration) {
    this.project = project;
    this.projectFileSystem = projectFileSystem;
    this.rulesProfile = rulesProfile;
    this.pmdProfileExporter = pmdProfileExporter;
    this.pmdConfiguration = pmdConfiguration;
  }

  public Report execute() {
    TimeProfiler profiler = new TimeProfiler().start("Execute PMD " + PmdVersion.getVersion());

    ClassLoader initialClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

      return executePmd();
    } finally {
      Thread.currentThread().setContextClassLoader(initialClassLoader);
      profiler.stop();
    }
  }

  private Report executePmd() {
    Report report = new Report();

    RuleContext context = new RuleContext();
    context.setReport(report);

    PmdTemplate pmdFactory = createPmdTemplate();
    executeRules(pmdFactory, context, projectFileSystem.mainFiles(Java.KEY), PmdConstants.REPOSITORY_KEY);
    executeRules(pmdFactory, context, projectFileSystem.testFiles(Java.KEY), PmdConstants.TEST_REPOSITORY_KEY);

    pmdConfiguration.dumpXmlReport(report);

    return report;
  }

  public void executeRules(PmdTemplate pmdFactory, RuleContext ruleContext, List<InputFile> files, String repositoryKey) {
    if (files.isEmpty()) {
      return; // Nothing to analyse
    }

    Charset encoding = projectFileSystem.getSourceCharset();
    RuleSets rulesets = createRulesets(repositoryKey);

    for (InputFile file : files) {
      pmdFactory.process(file.getFile(), encoding, rulesets, ruleContext);
    }
  }

  private RuleSets createRulesets(String repositoryKey) {
    String rulesXml = pmdProfileExporter.exportProfile(repositoryKey, rulesProfile);

    pmdConfiguration.dumpXmlRuleSet(repositoryKey, rulesXml);

    return new RuleSets(readRuleSet(rulesXml));
  }

  private static RuleSet readRuleSet(String rulesXml) {
    InputStream rulesInput = null;
    try {
      rulesInput = new ByteArrayInputStream(rulesXml.getBytes());

      return new RuleSetFactory().createRuleSet(rulesInput);
    } finally {
      Closeables.closeQuietly(rulesInput);
    }
  }

  @VisibleForTesting
  PmdTemplate createPmdTemplate() {
    return new PmdTemplate(JavaUtils.getSourceVersion(project));
  }
}
