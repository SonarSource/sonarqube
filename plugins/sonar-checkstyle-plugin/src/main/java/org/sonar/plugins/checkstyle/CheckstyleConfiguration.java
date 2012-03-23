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
package org.sonar.plugins.checkstyle;

import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.CoreProperties;
import org.sonar.api.Property;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.InputFileUtils;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.utils.SonarException;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

@org.sonar.api.Properties({
  @Property(
    key = CheckstyleConfiguration.PROPERTY_GENERATE_XML,
    defaultValue = "false",
    name = "Generate XML Report",
    project = false, global = false
  )
})
public class CheckstyleConfiguration implements BatchExtension {

  private static final Logger LOG = LoggerFactory.getLogger(CheckstyleConfiguration.class);
  public static final String PROPERTY_GENERATE_XML = "sonar.checkstyle.generateXml";

  private CheckstyleProfileExporter confExporter;
  private RulesProfile profile;
  private Settings conf;
  private ProjectFileSystem fileSystem;

  public CheckstyleConfiguration(Settings conf, CheckstyleProfileExporter confExporter, RulesProfile profile, ProjectFileSystem fileSystem) {
    this.conf = conf;
    this.confExporter = confExporter;
    this.profile = profile;
    this.fileSystem = fileSystem;
  }

  public File getXMLDefinitionFile() {
    Writer writer = null;
    File xmlFile = new File(fileSystem.getSonarWorkingDirectory(), "checkstyle.xml");
    try {
      writer = new OutputStreamWriter(new FileOutputStream(xmlFile, false), CharEncoding.UTF_8);
      confExporter.exportProfile(profile, writer);
      writer.flush();
      return xmlFile;

    } catch (IOException e) {
      throw new SonarException("Fail to save the Checkstyle configuration to " + xmlFile.getPath(), e);

    } finally {
      IOUtils.closeQuietly(writer);
    }
  }

  public List<File> getSourceFiles() {
    return InputFileUtils.toFiles(fileSystem.mainFiles(Java.KEY));
  }

  public File getTargetXMLReport() {
    if (conf.getBoolean(PROPERTY_GENERATE_XML)) {
      return new File(fileSystem.getSonarWorkingDirectory(), "checkstyle-result.xml");
    }
    return null;
  }

  public com.puppycrawl.tools.checkstyle.api.Configuration getCheckstyleConfiguration() throws IOException, CheckstyleException {
    File xmlConfig = getXMLDefinitionFile();

    LOG.info("Checkstyle configuration: " + xmlConfig.getAbsolutePath());
    com.puppycrawl.tools.checkstyle.api.Configuration configuration = toCheckstyleConfiguration(xmlConfig);
    defineCharset(configuration);
    return configuration;
  }

  static com.puppycrawl.tools.checkstyle.api.Configuration toCheckstyleConfiguration(File xmlConfig) throws CheckstyleException {
    return ConfigurationLoader.loadConfiguration(xmlConfig.getAbsolutePath(), new PropertiesExpander(new Properties()));
  }

  private void defineCharset(com.puppycrawl.tools.checkstyle.api.Configuration configuration) {
    com.puppycrawl.tools.checkstyle.api.Configuration[] modules = configuration.getChildren();
    for (com.puppycrawl.tools.checkstyle.api.Configuration module : modules) {
      if (("Checker".equals(module.getName()) || "com.puppycrawl.tools.checkstyle.Checker".equals(module.getName())) && (module instanceof DefaultConfiguration)) {
        Charset charset = getCharset();
        LOG.info("Checkstyle charset: " + charset.name());
        ((DefaultConfiguration) module).addAttribute("charset", charset.name());
      }
    }
  }

  public Locale getLocale() {
    return new Locale(conf.getString(CoreProperties.CORE_VIOLATION_LOCALE_PROPERTY));
  }

  public Charset getCharset() {
    Charset charset = fileSystem.getSourceCharset();
    if (charset == null) {
      charset = Charset.forName(System.getProperty("file.encoding", CharEncoding.UTF_8));
    }
    return charset;
  }
}
