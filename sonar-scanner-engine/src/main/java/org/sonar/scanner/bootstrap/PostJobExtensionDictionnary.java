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
package org.sonar.scanner.bootstrap;

import java.util.Collection;
import java.util.stream.Collectors;
import org.sonar.api.batch.postjob.PostJob;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.scanner.postjob.PostJobOptimizer;
import org.sonar.scanner.postjob.PostJobWrapper;

public class PostJobExtensionDictionnary extends AbstractExtensionDictionnary {

  private final PostJobContext postJobContext;
  private final PostJobOptimizer postJobOptimizer;

  public PostJobExtensionDictionnary(ComponentContainer componentContainer, PostJobOptimizer postJobOptimizer, PostJobContext postJobContext) {
    super(componentContainer);
    this.postJobOptimizer = postJobOptimizer;
    this.postJobContext = postJobContext;
  }

  public Collection<PostJobWrapper> selectPostJobs() {
    Collection<PostJob> result = sort(getFilteredExtensions(PostJob.class, null));
    return result.stream()
      .map(j -> new PostJobWrapper(j, postJobContext, postJobOptimizer))
      .filter(PostJobWrapper::shouldExecute)
      .collect(Collectors.toList());
  }
}
