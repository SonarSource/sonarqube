/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.scan.report;

import com.google.common.collect.Maps;
import freemarker.template.Template;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Settings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.Map;

@Properties({
  @Property(key = HtmlReport.HTML_REPORT_ENABLED_KEY, name = "Enable HTML report", description = "Set this to true to generate an HTML report",
    type = PropertyType.BOOLEAN, defaultValue = "false"),
  @Property(key = HtmlReport.HTML_REPORT_LOCATION_KEY, name = "HTML Report location",
    description = "Location of the generated report. Can be absolute or relative to working directory",
    type = PropertyType.STRING, defaultValue = HtmlReport.HTML_REPORT_LOCATION_DEFAULT, global = false, project = false),
  @Property(key = HtmlReport.HTML_REPORT_NAME_KEY, name = "HTML Report name",
    description = "Name of the generated report. Will be suffixed by .html or -light.html",
    type = PropertyType.STRING, defaultValue = HtmlReport.HTML_REPORT_NAME_DEFAULT, global = false, project = false),
  @Property(key = HtmlReport.HTML_REPORT_LIGHTMODE_ONLY, name = "Html report in light mode only", project = true,
    description = "Set this to true to only generate the new issues report (light report)",
    type = PropertyType.BOOLEAN, defaultValue = "false")})
public class HtmlReport implements Reporter {
  private static final Logger LOG = LoggerFactory.getLogger(HtmlReport.class);

  public static final String HTML_REPORT_ENABLED_KEY = "sonar.issuesReport.html.enable";
  public static final String HTML_REPORT_LOCATION_KEY = "sonar.issuesReport.html.location";
  public static final String HTML_REPORT_LOCATION_DEFAULT = "issues-report";
  public static final String HTML_REPORT_NAME_KEY = "sonar.issuesReport.html.name";
  public static final String HTML_REPORT_NAME_DEFAULT = "issues-report";
  public static final String HTML_REPORT_LIGHTMODE_ONLY = "sonar.issuesReport.lightModeOnly";

  private final Settings settings;
  private final FileSystem fs;
  private final IssuesReportBuilder builder;
  private final SourceProvider sourceProvider;
  private final RuleNameProvider ruleNameProvider;

  public HtmlReport(Settings settings, FileSystem fs, IssuesReportBuilder builder, SourceProvider sourceProvider, RuleNameProvider ruleNameProvider) {
    this.settings = settings;
    this.fs = fs;
    this.builder = builder;
    this.sourceProvider = sourceProvider;
    this.ruleNameProvider = ruleNameProvider;
  }

  @Override
  public void execute() {
    if (settings.getBoolean(HTML_REPORT_ENABLED_KEY)) {
      IssuesReport report = builder.buildReport();
      print(report);
    }
  }

  public void print(IssuesReport report) {
    File reportFileDir = getReportFileDir();
    String reportName = settings.getString(HTML_REPORT_NAME_KEY);
    if (!isLightModeOnly()) {
      File reportFile = new File(reportFileDir, reportName + ".html");
      LOG.debug("Generating HTML Report to: " + reportFile.getAbsolutePath());
      writeToFile(report, reportFile, true);
      LOG.info("HTML Issues Report generated: " + reportFile.getAbsolutePath());
    }
    File lightReportFile = new File(reportFileDir, reportName + "-light.html");
    LOG.debug("Generating Light HTML Report to: " + lightReportFile.getAbsolutePath());
    writeToFile(report, lightReportFile, false);
    LOG.info("Light HTML Issues Report generated: " + lightReportFile.getAbsolutePath());
    try {
      copyDependencies(reportFileDir);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to copy HTML report resources to: " + reportFileDir, e);
    }
  }

  private File getReportFileDir() {
    String reportFileDirStr = settings.getString(HTML_REPORT_LOCATION_KEY);
    File reportFileDir = new File(reportFileDirStr);
    if (!reportFileDir.isAbsolute()) {
      reportFileDir = new File(fs.workDir(), reportFileDirStr);
    }
    if (reportFileDirStr.endsWith(".html")) {
      LOG.warn(HTML_REPORT_LOCATION_KEY + " should indicate a directory. Using parent folder.");
      reportFileDir = reportFileDir.getParentFile();
    }
    try {
      FileUtils.forceMkdir(reportFileDir);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create the directory " + reportFileDirStr, e);
    }
    return reportFileDir;
  }

  public void writeToFile(IssuesReport report, File toFile, boolean complete) {
    Writer writer = null;
    FileOutputStream fos = null;
    try {
      freemarker.log.Logger.selectLoggerLibrary(freemarker.log.Logger.LIBRARY_NONE);
      freemarker.template.Configuration cfg = new freemarker.template.Configuration();
      cfg.setClassForTemplateLoading(HtmlReport.class, "");

      Map<String, Object> root = Maps.newHashMap();
      root.put("report", report);
      root.put("ruleNameProvider", ruleNameProvider);
      root.put("sourceProvider", sourceProvider);
      root.put("complete", complete);

      Template template = cfg.getTemplate("issuesreport.ftl");
      fos = new FileOutputStream(toFile);
      writer = new OutputStreamWriter(fos, fs.encoding());
      template.process(root, writer);
      writer.flush();

    } catch (Exception e) {
      throw new IllegalStateException("Fail to generate HTML Issues Report to: " + toFile, e);

    } finally {
      IOUtils.closeQuietly(writer);
      IOUtils.closeQuietly(fos);
    }
  }

  void copyDependencies(File toDir) throws URISyntaxException, IOException {
    File target = new File(toDir, "issuesreport_files");
    FileUtils.forceMkdir(target);

    // I don't know how to extract a directory from classpath, that's why an exhaustive list of files
    // is provided here :
    copyDependency(target, "sonar.eot");
    copyDependency(target, "sonar.svg");
    copyDependency(target, "sonar.ttf");
    copyDependency(target, "sonar.woff");
    copyDependency(target, "favicon.ico");
    copyDependency(target, "PRJ.png");
    copyDependency(target, "DIR.png");
    copyDependency(target, "FIL.png");
    copyDependency(target, "jquery.min.js");
    copyDependency(target, "sep12.png");
    copyDependency(target, "sonar.css");
    copyDependency(target, "sonarqube-24x100.png");
  }

  private void copyDependency(File target, String filename) {
    InputStream input = null;
    OutputStream output = null;
    try {
      input = getClass().getResourceAsStream("/org/sonar/batch/scan/report/issuesreport_files/" + filename);
      output = new FileOutputStream(new File(target, filename));
      IOUtils.copy(input, output);

    } catch (IOException e) {
      throw new IllegalStateException("Fail to copy file " + filename + " to " + target, e);
    } finally {
      IOUtils.closeQuietly(input);
      IOUtils.closeQuietly(output);
    }
  }

  public boolean isLightModeOnly() {
    return settings.getBoolean(HTML_REPORT_LIGHTMODE_ONLY);
  }
}
