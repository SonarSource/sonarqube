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
package org.sonar.db;

import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

/**
 * This class should be called using @Rule.
 * Data is truncated between each tests. The schema is created between each test.
 */
public class CoreDbTester extends AbstractDbTester<CoreTestDb> {
  private final DefaultOrganizationTesting defaultOrganizationTesting;

  private CoreDbTester(@Nullable String schemaPath) {
    super(CoreTestDb.create(schemaPath));
    this.defaultOrganizationTesting = new DefaultOrganizationTesting(this);
  }

  public static CoreDbTester createForSchema(Class testClass, String filename) {
    String path = StringUtils.replaceChars(testClass.getCanonicalName(), '.', '/');
    String schemaPath = path + "/" + filename;
    return new CoreDbTester(schemaPath);
  }

  public static CoreDbTester createEmpty() {
    String path = StringUtils.replaceChars(CoreDbTester.class.getCanonicalName(), '.', '/');
    String schemaPath = path + "/empty.sql";
    return new CoreDbTester(schemaPath);
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

  public DefaultOrganizationTesting defaultOrganization() {
    return defaultOrganizationTesting;
  }
}
