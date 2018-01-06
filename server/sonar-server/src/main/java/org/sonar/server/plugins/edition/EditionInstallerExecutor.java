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
package org.sonar.server.plugins.edition;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.sonar.server.util.AbstractStoppableExecutorService;

public class EditionInstallerExecutor extends AbstractStoppableExecutorService<ExecutorService> {
  public EditionInstallerExecutor() {
    super(createExecutor());
  }

  @Override
  public void execute(Runnable r) {
    delegate.submit(r);
  }

  private static ExecutorService createExecutor() {
    ThreadFactory threadFactory = new ThreadFactoryBuilder()
      .setDaemon(true)
      .setNameFormat("edition-installation-%d")
      .build();
    return Executors.newSingleThreadExecutor(threadFactory);
  }
}
