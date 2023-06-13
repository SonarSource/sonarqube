/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scm.git.strategy;

import org.sonar.api.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonar.scm.git.strategy.DefaultBlameStrategy.BlameAlgorithmEnum.*;

/**
 * The blame strategy defines when the files-blame algorithm is used over jgit or native git algorithm.
 * It has been found that the JGit/Git native algorithm performs better in certain circumstances, such as:
 * - When we have many cores available for multi-threading
 * - The number of files to be blame by the algorithm
 * This two metrics are correlated:
 * - The more threads available, the more it is favorable to use the JGit/Git native algorithm
 * - The more files available, the more it is favorable to use the git-files-blame algorithm
 */
public class DefaultBlameStrategy implements BlameStrategy {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultBlameStrategy.class);
  private static final int FILES_GIT_BLAME_TRIGGER = 10;
  public static final String PROP_SONAR_SCM_USE_BLAME_ALGORITHM = "sonar.scm.use.blame.algorithm";
  private final Configuration configuration;

  public DefaultBlameStrategy(Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public BlameAlgorithmEnum getBlameAlgorithm(int availableProcessors, int numberOfFiles) {
    BlameAlgorithmEnum forcedStrategy = configuration.get(PROP_SONAR_SCM_USE_BLAME_ALGORITHM)
      .map(BlameAlgorithmEnum::valueOf)
      .orElse(null);

    if (forcedStrategy != null) {
      return forcedStrategy;
    }

    if (availableProcessors == 0) {
      LOG.warn("Available processors are 0. Falling back to native git blame");
      return GIT_NATIVE_BLAME;
    }
    if (numberOfFiles / availableProcessors > FILES_GIT_BLAME_TRIGGER) {
      return GIT_FILES_BLAME;
    }

    return GIT_NATIVE_BLAME;
  }

  public enum BlameAlgorithmEnum {
    /**
     * Strategy using native git for the blame, or JGit on single file as a fallback
     */
    GIT_NATIVE_BLAME,
    /**
     * Strategy using git-files-blame algorithm
     */
    GIT_FILES_BLAME;
  }

}
