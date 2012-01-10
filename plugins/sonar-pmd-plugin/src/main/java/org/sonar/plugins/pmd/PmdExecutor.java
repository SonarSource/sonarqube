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

import net.sourceforge.pmd.*;
import net.sourceforge.pmd.renderers.Renderer;
import net.sourceforge.pmd.renderers.XMLRenderer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.java.api.JavaUtils;

import java.io.*;
import java.util.List;

public class PmdExecutor implements BatchExtension {

  private static final Logger LOG = LoggerFactory.getLogger(PmdExecutor.class);

  private PmdConfiguration configuration;
  private Project project;

  public PmdExecutor(Project project, PmdConfiguration configuration) {
    this.project = project;
    this.configuration = configuration;
  }

  public File execute() throws IOException, PMDException {
    TimeProfiler profiler = new TimeProfiler().start("Execute PMD " + PmdVersion.getVersion());

    ClassLoader initialClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
    try {
      PMD pmd = new PMD();
      setJavaVersion(pmd, project);
      RuleContext ruleContext = new RuleContext();
      Report report = new Report();
      ruleContext.setReport(report);

      RuleSets rulesets = createRulesets();

      for (File file : project.getFileSystem().getSourceFiles(Java.INSTANCE)) {
        ruleContext.setSourceCodeFilename(file.getAbsolutePath());
        Reader fileReader = new InputStreamReader(new FileInputStream(file), project.getFileSystem().getSourceCharset());
        try {
          pmd.processFile(fileReader, rulesets, ruleContext);

        } catch (PMDException e) {
          LOG.error("Fail to execute PMD. Following file is ignored: " + file, e.getCause());

        } catch (Exception e) {
          LOG.error("Fail to execute PMD. Following file is ignored: " + file, e);

        } finally {
          IOUtils.closeQuietly(fileReader);
        }
      }

      return writeXmlReport(project, report);

    } finally {
      profiler.stop();
      Thread.currentThread().setContextClassLoader(initialClassLoader);
    }
  }

  private RuleSets createRulesets() {
    RuleSets rulesets = new RuleSets();
    RuleSetFactory ruleSetFactory = new RuleSetFactory();

    List<String> rulesetPaths = configuration.getRulesets();
    LOG.info("PMD configuration: " + StringUtils.join(rulesetPaths, ", "));

    for (String rulesetPath : rulesetPaths) {
      InputStream rulesInput = openRuleset(rulesetPath);
      rulesets.addRuleSet(ruleSetFactory.createRuleSet(rulesInput));
      IOUtils.closeQuietly(rulesInput);
    }
    return rulesets;
  }

  private InputStream openRuleset(String rulesetPath) {
    try {
      File file = new File(rulesetPath);
      boolean found;
      if (file.exists()) {
        found = true;
      } else {
        file = new File(project.getFileSystem().getBasedir(), rulesetPath);
        found = file.exists();
      }
      if (found) {
        return new FileInputStream(file);
      }
      InputStream stream = getClass().getResourceAsStream(rulesetPath);
      if (stream == null) {
        throw new RuntimeException("The PMD ruleset can not be found: " + rulesetPath);
      }
      return stream;

    } catch (FileNotFoundException e) {
      throw new SonarException("The PMD ruleset can not be found: " + rulesetPath, e);
    }
  }

  private File writeXmlReport(Project project, Report report) throws IOException {
    Renderer xmlRenderer = new XMLRenderer();
    Writer stringwriter = new StringWriter();
    xmlRenderer.setWriter(stringwriter);
    xmlRenderer.start();
    xmlRenderer.renderFileReport(report);
    xmlRenderer.end();

    File xmlReport = new File(project.getFileSystem().getSonarWorkingDirectory(), "pmd-result.xml");
    LOG.info("PMD output report: " + xmlReport.getAbsolutePath());
    FileUtils.write(xmlReport, stringwriter.toString());
    return xmlReport;
  }

  static String getNormalizedJavaVersion(String javaVersion) {
    if (StringUtils.equals("1.1", javaVersion) || StringUtils.equals("1.2", javaVersion)) {
      javaVersion = "1.3";
    } else if (StringUtils.equals("5", javaVersion)) {
      javaVersion = "1.5";
    } else if (StringUtils.equals("6", javaVersion)) {
      javaVersion = "1.6";
    }
    return javaVersion;
  }

  private void setJavaVersion(PMD pmd, Project project) {
    String javaVersion = getNormalizedJavaVersion(JavaUtils.getSourceVersion(project));
    if (javaVersion != null) {
      SourceType sourceType = SourceType.getSourceTypeForId("java " + javaVersion);
      if (sourceType != null) {
        LOG.info("Java version: " + javaVersion);
        pmd.setJavaVersion(sourceType);
      } else {
        throw new SonarException("Unsupported Java version for PMD: " + javaVersion);
      }
    }
  }
}
