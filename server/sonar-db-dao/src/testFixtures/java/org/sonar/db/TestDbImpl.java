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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.AssumptionViolatedException;
import org.sonar.api.config.Settings;
import org.sonar.db.dialect.H2;
import org.sonar.process.logging.LogbackHelper;

class TestDbImpl extends CoreTestDb {
  private static TestDbImpl defaultSchemaBaseTestDb;
  // instantiating MyBatis objects is costly => we cache them for default schema
  private static final Map<MyBatisConfExtension[], TestDbImpl> defaultSchemaTestDbsWithExtensions = new HashMap<>();

  private boolean isDefault;
  private MyBatis myBatis;

  private TestDbImpl(@Nullable String schemaPath, MyBatisConfExtension... confExtensions) {
    super();
    isDefault = (schemaPath == null);
    init(schemaPath, confExtensions);
  }

  private TestDbImpl(TestDbImpl base, MyBatis myBatis) {
    super(base.getDatabase());
    this.isDefault = base.isDefault;
    this.myBatis = myBatis;
  }

  private void init(@Nullable String schemaPath, MyBatisConfExtension[] confExtensions) {
    Consumer<Settings> loadOrchestratorSettings = settings -> {
      OrchestratorSettingsUtils.loadOrchestratorSettings(settings);
    };
    Function<Settings, Database> databaseCreator = settings -> {
      String dialect = settings.getString("sonar.jdbc.dialect");
      if (dialect != null && !"h2".equals(dialect)) {
        return new DefaultDatabase(new LogbackHelper(), settings);
      }
      return SQDatabase.newH2Database("h2Tests" + DigestUtils.md5Hex(StringUtils.defaultString(schemaPath)), schemaPath == null);
    };
    Consumer<Database> schemaPathExecutor = database -> {
      if (schemaPath == null) {
        return;
      }

      // scripts are assumed to be using H2 specific syntax, ignore the test if not on H2
      if (!database.getDialect().getId().equals("h2")) {
        database.stop();
        throw new AssumptionViolatedException("This test is intended to be run on H2 only");
      }
      ((SQDatabase) database).executeScript(schemaPath);
    };
    BiConsumer<Database, Boolean> createMyBatis = (db, created) -> myBatis = newMyBatis(db, confExtensions);
    init(loadOrchestratorSettings, databaseCreator, schemaPathExecutor, createMyBatis);
  }

  private static MyBatis newMyBatis(Database db, MyBatisConfExtension[] extensions) {
    MyBatis newMyBatis = new MyBatis(db, extensions);
    newMyBatis.start();
    return newMyBatis;
  }

  static TestDbImpl create(@Nullable String schemaPath, MyBatisConfExtension... confExtensions) {
    MyBatisConfExtension[] extensionArray = confExtensions.length == 0 ? null : confExtensions;
    if (schemaPath == null) {
      if (defaultSchemaBaseTestDb == null) {
        defaultSchemaBaseTestDb = new TestDbImpl((String) null);
      }
      if (extensionArray != null) {
        return defaultSchemaTestDbsWithExtensions.computeIfAbsent(
          extensionArray,
          extensions -> new TestDbImpl(defaultSchemaBaseTestDb, newMyBatis(defaultSchemaBaseTestDb.getDatabase(), extensions)));
      }
      return defaultSchemaBaseTestDb;
    }
    return new TestDbImpl(schemaPath, confExtensions);
  }

  @Override
  public void start() {
    if (!isDefault && !H2.ID.equals(getDatabase().getDialect().getId())) {
      throw new AssumptionViolatedException("Test disabled because it supports only H2");
    }
  }

  @Override
  public void stop() {
    if (!isDefault) {
      super.stop();
    }
  }

  MyBatis getMyBatis() {
    return myBatis;
  }
}
