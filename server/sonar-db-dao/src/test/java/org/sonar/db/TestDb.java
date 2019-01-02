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

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

class TestDb extends CoreTestDb {

  private static TestDb defaultSchemaBaseTestDb;
  // instantiating MyBatis objects is costly => we cache them for default schema
  private static final Map<MyBatisConfExtension[], TestDb> defaultSchemaTestDbsWithExtensions = new HashMap<>();

  private MyBatis myBatis;

  private TestDb(@Nullable String schemaPath, MyBatisConfExtension... confExtensions) {
    super();
    init(schemaPath, (db, created) -> myBatis = newMyBatis(db, confExtensions));
  }

  private TestDb(TestDb base, MyBatis myBatis) {
    super(base);
    this.myBatis = myBatis;
  }

  private static MyBatis newMyBatis(Database db, MyBatisConfExtension[] extensions) {
    MyBatis newMyBatis = new MyBatis(db, extensions);
    newMyBatis.start();
    return newMyBatis;
  }

  static TestDb create(@Nullable String schemaPath, MyBatisConfExtension... confExtensions) {
    MyBatisConfExtension[] extensionArray = confExtensions == null || confExtensions.length == 0 ? null : confExtensions;
    if (schemaPath == null) {
      if (defaultSchemaBaseTestDb == null) {
        defaultSchemaBaseTestDb = new TestDb((String) null);
      }
      if (extensionArray != null) {
        return defaultSchemaTestDbsWithExtensions.computeIfAbsent(
          extensionArray,
          extensions -> new TestDb(defaultSchemaBaseTestDb, newMyBatis(defaultSchemaBaseTestDb.getDatabase(), extensions)));
      }
      return defaultSchemaBaseTestDb;
    }
    return new TestDb(schemaPath, confExtensions);
  }

  MyBatis getMyBatis() {
    return myBatis;
  }
}
