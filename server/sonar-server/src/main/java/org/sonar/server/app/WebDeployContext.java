/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.app;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import javax.servlet.ServletException;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import static java.lang.String.format;

public class WebDeployContext {

  public static final String RELATIVE_DIR_IN_DATA = "web/deploy";
  private final Fs fs;

  public WebDeployContext() {
    this(new Fs());
  }

  @VisibleForTesting
  public WebDeployContext(Fs fs) {
    this.fs = fs;
  }

  public void configureTomcat(Tomcat tomcat, Props props) throws ServletException {
    File deployDir = new File(props.nonNullValueAsFile(ProcessProperties.PATH_DATA), RELATIVE_DIR_IN_DATA);
    try {
      fs.createOrCleanupDir(deployDir);
    } catch (IOException e) {
      throw new IllegalStateException(format("Fail to create or clean-up directory %s", deployDir.getAbsolutePath()), e);
    }
    tomcat.addWebapp("/deploy", deployDir.getAbsolutePath());
  }

  static class Fs {
    void createOrCleanupDir(File dir) throws IOException {
      FileUtils.forceMkdir(dir);
      FileUtils.cleanDirectory(dir);
    }
  }
}
