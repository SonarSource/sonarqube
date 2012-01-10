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

import org.apache.commons.io.FileUtils;
import org.jacoco.agent.AgentJar;
import org.jacoco.core.JaCoCo;
import org.sonar.api.BatchExtension;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.io.IOException;

/**
 * @author Evgeny Mandrikov
 */
public class JaCoCoAgentDownloader implements BatchExtension {

  /**
   * Dirty hack, but it allows to extract agent only once during Sonar analyzes for multi-module project.
   */
  private static File agentJarFile;

  public JaCoCoAgentDownloader() {
  }

  public synchronized File getAgentJarFile() {
    if (agentJarFile == null) {
      agentJarFile = extractAgent();
    }
    return agentJarFile;
  }

  private File extractAgent() {
    try {
      File agent = File.createTempFile("jacocoagent", ".jar");
      AgentJar.extractTo(agent);
      FileUtils.forceDeleteOnExit(agent); // TODO evil method
      JaCoCoUtils.LOG.info("JaCoCo agent (version " + JaCoCo.VERSION + ") extracted: {}", agent);
      return agent;
    } catch (IOException e) {
      throw new SonarException(e);
    }
  }
}
