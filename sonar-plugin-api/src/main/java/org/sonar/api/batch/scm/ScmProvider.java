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
package org.sonar.api.batch.scm;

import org.sonar.api.BatchSide;
import org.sonar.api.CoreProperties;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.batch.InstantiationStrategy;

import java.io.File;

/**
 * See {@link CoreProperties#LINKS_SOURCES_DEV} to get old Maven URL format.
 * @since 5.0
 */
@BatchSide
@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
@ExtensionPoint
public abstract class ScmProvider {

  /**
   * Unique identifier of the provider. Can be passed to {@link CoreProperties#SCM_PROVIDER_KEY}
   * Can be used in SCM URL to define the provider to use.
   */
  public abstract String key();

  /**
   * Does this provider able to manage files located in this directory.
   * Used by autodetection. Not considered if user has forced the provider key.
   * @return false by default
   */
  public boolean supports(File baseDir) {
    return false;
  }

  public BlameCommand blameCommand() {
    throw new UnsupportedOperationException("Blame command is not supported by " + key() + " provider");
  }

}
