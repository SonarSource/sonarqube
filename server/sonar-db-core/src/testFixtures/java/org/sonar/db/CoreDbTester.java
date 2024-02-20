/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * This class should be called using @Rule.
 * Data is truncated between each tests. The schema is created between each test.
 */
public class CoreDbTester extends AbstractDbTester<CoreTestDb> implements BeforeEachCallback, AfterEachCallback {

  private CoreDbTester(CoreTestDb testDb) {
    super(testDb);
  }

  public static CoreDbTester createForSchema(Class testClass, String filename) {
    return createForSchema(testClass, filename, true);
  }

  public static CoreDbTester createForSchema(Class testClass, String filename, boolean databaseToUpper) {
    String path = StringUtils.replaceChars(testClass.getCanonicalName(), '.', '/');
    String schemaPath = path + "/" + filename;
    return new CoreDbTester(CoreTestDb.create(schemaPath, databaseToUpper));
  }

  public static CoreDbTester createEmpty() {
    return new CoreDbTester(CoreTestDb.createEmpty());
  }

  @Override
  protected void before() {
    db.start();
    db.truncateTables();
  }

  @Override
  protected void after() {
    db.stop();
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) throws Exception {
    after();
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) throws Exception {
    before();
  }
}
