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

import org.sonar.api.batch.postjob.PostJob;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.internal.DefaultPostJobDescriptor;

public class PostJobWrapper {

  private PostJob wrappedPostJob;
  private PostJobContext adaptor;
  private DefaultPostJobDescriptor descriptor;
  private PostJobOptimizer optimizer;

  public PostJobWrapper(PostJob newPostJob, PostJobContext adaptor, PostJobOptimizer optimizer) {
    this.wrappedPostJob = newPostJob;
    this.optimizer = optimizer;
    this.descriptor = new DefaultPostJobDescriptor();
    newPostJob.describe(descriptor);
    if (descriptor.name() == null) {
      descriptor.name(newPostJob.getClass().getName());
    }
    this.adaptor = adaptor;
  }

  public boolean shouldExecute() {
    return optimizer.shouldExecute(descriptor);
  }

  public void execute() {
    wrappedPostJob.execute(adaptor);
  }

  @Override
  public String toString() {
    return descriptor.name();
  }
}
