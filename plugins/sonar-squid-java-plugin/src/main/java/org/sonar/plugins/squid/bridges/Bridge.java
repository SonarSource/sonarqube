/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.plugins.squid.bridges;

import org.sonar.api.batch.SensorContext;
import org.sonar.api.checks.checkers.MessageDispatcher;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.squid.Squid;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourcePackage;
import org.sonar.squid.api.SourceProject;

/**
 * Pattern visitor : project -> packages -> files
 */
public abstract class Bridge {

  boolean needsBytecode = false;
  Squid squid;
  ResourceIndex resourceIndex;
  SensorContext context;
  MessageDispatcher messageDispatcher;

  protected Bridge(boolean needsBytecode) {
    this.needsBytecode = needsBytecode;
  }

  public final boolean needsBytecode() {
    return needsBytecode;
  }

  protected final void setSquid(Squid squid) {
    this.squid = squid;
  }

  protected final void setMessageDispatcher(MessageDispatcher messageDispatcher) {
    this.messageDispatcher = messageDispatcher;
  }

  protected final void setResourceIndex(ResourceIndex resourceIndex) {
    this.resourceIndex = resourceIndex;
  }

  protected final void setContext(SensorContext context) {
    this.context = context;
  }

  public void onProject(SourceProject squidProject, Project sonarProject) {

  }

  public void onPackage(SourcePackage squidPackage, Resource sonarPackage) {

  }

  public void onFile(SourceFile squidFile, Resource sonarFile) {

  }
}
