/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.xoo.extensions;

import com.google.common.collect.Iterables;
import org.sonar.api.batch.postjob.PostJob;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.PostJobDescriptor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class XooPostJob implements PostJob {

  private static final Logger LOG = Loggers.get(XooPostJob.class);

  @Override
  public void describe(PostJobDescriptor descriptor) {
    descriptor.name("Xoo Post Job")
      .requireProperty("sonar.xoo.enablePostJob");

  }

  @Override
  public void execute(PostJobContext context) {
    LOG.info("Resolved issues: " + Iterables.size(context.resolvedIssues()));
    LOG.info("Open issues: " + Iterables.size(context.issues()));
  }

}
