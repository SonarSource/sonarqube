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
package org.sonar.api.batch.fs.internal;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

@Immutable
public abstract class AbstractProjectOrModule extends DefaultInputComponent {
  private static final Logger LOGGER = Loggers.get(AbstractProjectOrModule.class);
  private final Path baseDir;
  private final Path workDir;
  private final String name;
  private final String originalName;
  private final String description;
  private final String keyWithBranch;
  private final String branch;
  private final Map<String, String> properties;

  private final String key;
  private final ProjectDefinition definition;
  private final Charset encoding;

  public AbstractProjectOrModule(ProjectDefinition definition, int scannerComponentId) {
    super(scannerComponentId);
    this.baseDir = initBaseDir(definition);
    this.workDir = initWorkingDir(definition);
    this.name = definition.getName();
    this.originalName = definition.getOriginalName();
    this.description = definition.getDescription();
    this.keyWithBranch = definition.getKeyWithBranch();
    this.branch = definition.getBranch();
    this.properties = Collections.unmodifiableMap(new HashMap<>(definition.properties()));

    this.definition = definition;
    this.key = definition.getKey();
    this.encoding = initEncoding(definition);
  }

  private static Charset initEncoding(ProjectDefinition module) {
    String encodingStr = module.properties().get(CoreProperties.ENCODING_PROPERTY);
    Charset result;
    if (StringUtils.isNotEmpty(encodingStr)) {
      result = Charset.forName(StringUtils.trim(encodingStr));
    } else {
      result = Charset.defaultCharset();
    }
    return result;
  }

  private static Path initBaseDir(ProjectDefinition module) {
    Path result;
    try {
      result = module.getBaseDir().toPath().toRealPath(LinkOption.NOFOLLOW_LINKS);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to resolve module baseDir", e);
    }
    return result;
  }

  private static Path initWorkingDir(ProjectDefinition module) {
    File workingDirAsFile = module.getWorkDir();
    Path workingDir = workingDirAsFile.getAbsoluteFile().toPath().normalize();
    if (SystemUtils.IS_OS_WINDOWS) {
      try {
        Files.createDirectories(workingDir);
        Files.setAttribute(workingDir, "dos:hidden", true, LinkOption.NOFOLLOW_LINKS);
      } catch (IOException e) {
        LOGGER.warn("Failed to set working directory hidden: {}", e.getMessage());
      }
    }
    return workingDir;
  }

  /**
   * Module key without branch
   */
  @Override
  public String key() {
    return key;
  }

  @Override
  public boolean isFile() {
    return false;
  }

  public ProjectDefinition definition() {
    return definition;
  }

  public Path getBaseDir() {
    return baseDir;
  }

  public Path getWorkDir() {
    return workDir;
  }

  public String getKeyWithBranch() {
    return keyWithBranch;
  }

  @CheckForNull
  public String getBranch() {
    return branch;
  }

  public Map<String, String> properties() {
    return properties;
  }

  @CheckForNull
  public String getOriginalName() {
    return originalName;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Charset getEncoding() {
    return encoding;
  }
}
