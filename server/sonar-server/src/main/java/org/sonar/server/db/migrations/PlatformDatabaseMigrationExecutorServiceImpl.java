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
package org.sonar.server.db.migrations;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.sonar.server.util.AbstractStoppableExecutorService;

import java.util.concurrent.Executors;

/**
 * Since only one DB migration can run at a time, this implementation of PlatformDatabaseMigrationExecutorService
 * wraps a single thread executor from the JDK.
 */
public class PlatformDatabaseMigrationExecutorServiceImpl
  extends AbstractStoppableExecutorService
  implements PlatformDatabaseMigrationExecutorService {

  public PlatformDatabaseMigrationExecutorServiceImpl() {
    super(
      Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder()
          .setDaemon(false)
          .setNameFormat("DB_migration-%d")
          .build()
        ));
  }
}
