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
package org.sonar.plugins.checkstyle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Properties;

import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;

public class CheckstyleConfiguration implements BatchExtension {

  private static final Logger LOG = LoggerFactory.getLogger(CheckstyleConfiguration.class);

  private CheckstyleProfileExporter confExporter;
  private RulesProfile profile;
  private Project project;

  public CheckstyleConfiguration(CheckstyleProfileExporter confExporter, RulesProfile profile, Project project) {
    this.confExporter = confExporter;
    this.profile = profile;
    this.project = project;
  }

  public File getXMLDefinitionFile() {
    Writer writer = null;
    File xmlFile = new File(project.getFileSystem().getSonarWorkingDirectory(), "checkstyle.xml");
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
    return project.getFileSystem().getSourceFiles(Java.INSTANCE);
  }

  public File getTargetXMLReport() {
    if (project.getConfiguration().getBoolean(CheckstyleConstants.GENERATE_XML_KEY, CheckstyleConstants.GENERATE_XML_DEFAULT_VALUE)) {
      return new File(project.getFileSystem().getSonarWorkingDirectory(), "checkstyle-result.xml");
    }
    return null;
  }

  public Configuration getCheckstyleConfiguration() throws IOException, CheckstyleException {
    File xmlConfig = getXMLDefinitionFile();

    LOG.info("Checkstyle configuration: " + xmlConfig.getAbsolutePath());
    Configuration configuration = toCheckstyleConfiguration(xmlConfig);
    defineCharset(configuration);
    return configuration;
  }

  static Configuration toCheckstyleConfiguration(File xmlConfig) throws CheckstyleException {
    return ConfigurationLoader.loadConfiguration(xmlConfig.getAbsolutePath(), new PropertiesExpander(new Properties()));
  }

  private void defineCharset(Configuration configuration) {
    Configuration[] modules = configuration.getChildren();
    for (Configuration module : modules) {
      if ("Checker".equals(module.getName()) || "com.puppycrawl.tools.checkstyle.Checker".equals(module.getName())) {
        if (module instanceof DefaultConfiguration) {
          Charset charset = getCharset();
          LOG.info("Checkstyle charset: " + charset.name());
          ((DefaultConfiguration) module).addAttribute("charset", charset.name());
        }
      }
    }
  }

  public Charset getCharset() {
    Charset charset = project.getFileSystem().getSourceCharset();
    if (charset == null) {
      charset = Charset.forName(System.getProperty("file.encoding", CharEncoding.UTF_8));
    }
    return charset;
  }
}
