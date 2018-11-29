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
package org.sonar.scanner.postjob;

import java.util.Collection;
import java.util.stream.Collectors;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.bootstrap.PostJobExtensionDictionnary;

public class PostJobsExecutor {
  private static final Logger LOG = Loggers.get(PostJobsExecutor.class);

  private final PostJobExtensionDictionnary selector;

  public PostJobsExecutor(PostJobExtensionDictionnary selector) {
    this.selector = selector;
  }

  public void execute() {
    Collection<PostJobWrapper> postJobs = selector.selectPostJobs();
    execute(postJobs);
  }

  private static void execute(Collection<PostJobWrapper> postJobs) {
    logPostJobs(postJobs);

    for (PostJobWrapper postJob : postJobs) {
      if (postJob.shouldExecute()) {
        LOG.info("Executing post-job '{}'", postJob);
        postJob.execute();
      }
    }
  }

  private static void logPostJobs(Collection<PostJobWrapper> postJobs) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(() -> "Post-jobs : " + postJobs.stream().map(Object::toString).collect(Collectors.joining(" -> ")));
    }
  }
}
