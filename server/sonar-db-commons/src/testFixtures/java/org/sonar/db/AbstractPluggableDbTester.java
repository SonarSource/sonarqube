/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db;

/**
 * Base class for database test fixtures that use ServiceLoader to dynamically load any TestDb implementation.
 * Extends {@link AbstractDbTester} but uses the base {@link TestDb} type rather than a specific subtype,
 * since the actual TestDb type is determined at runtime via ServiceLoader.
 *
 * <p>Use this class when you want your tester to be pluggable via ServiceLoader and work with any TestDb provider.
 * For testers that need a specific TestDb subtype (like DbTester needing TestDbImpl), extend {@link AbstractDbTester} directly.
 */
public abstract class AbstractPluggableDbTester extends AbstractDbTester<TestDb> {
  /**
   * Constructor using ServiceLoader to create TestDb.
   * Subclasses can use this to delegate TestDb creation to the ServiceLoader.
   * The type parameter must be fixed to TestDb and not a subtype, since it is now
   * dynamically loaded.
   */
  protected AbstractPluggableDbTester() {
    super(createTestDb());
  }

  private static TestDb createTestDb() {
    TestDbProvider provider = ServiceLoaderWrapper.loadSingletonService(TestDbProvider.class);
    return provider.create();
  }
}
