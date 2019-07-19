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

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.dbunit.DataSourceDatabaseTester;
import org.dbunit.IDatabaseTester;
import org.junit.AssumptionViolatedException;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.dialect.H2;
import org.sonar.process.logging.LogbackHelper;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.sonar.process.ProcessProperties.Property.JDBC_USERNAME;

/**
 * This class should be call using @ClassRule in order to create the schema once (if @Rule is used
 * the schema will be recreated before each test).
 */
class CoreTestDb implements TestDb {

  private Database db;
  private DatabaseCommands commands;
  private IDatabaseTester tester;

  protected CoreTestDb() {
    // use static factory method
  }
  protected CoreTestDb(Database db, DatabaseCommands commands, IDatabaseTester tester) {
    this.db = db;
    this.commands = commands;
    this.tester = tester;
  }

  static CoreTestDb create(String schemaPath) {
    requireNonNull(schemaPath, "schemaPath can't be null");

    return new CoreTestDb().init(schemaPath);
  }

  static CoreTestDb createEmpty() {
    return new CoreTestDb().init(null);
  }

  private CoreTestDb init(@Nullable String schemaPath) {
    Function<Settings, Database> databaseCreator = settings -> {
      String dialect = settings.getString("sonar.jdbc.dialect");
      if (dialect != null && !"h2".equals(dialect)) {
        return new DefaultDatabase(new LogbackHelper(), settings);
      }
      return new CoreH2Database("h2Tests-" + (schemaPath == null ?  "empty" : DigestUtils.md5Hex(schemaPath)));
    };
    Consumer<Database> databaseInitializer = database -> {
      if (schemaPath == null) {
        return;
      }

      // scripts are assumed to be using H2 specific syntax, ignore the test if not on H2
      if (!database.getDialect().getId().equals("h2")) {
        database.stop();
        throw new AssumptionViolatedException("This test is intended to be run on H2 only");
      }

      ((CoreH2Database) database).executeScript(schemaPath);
    };
    BiConsumer<Database, Boolean> noPostStartAction = (db, created) -> {
    };

    init(databaseCreator, databaseInitializer, noPostStartAction);
    return this;
  }

  protected void init(Function<Settings, Database> databaseCreator,
    Consumer<Database> databaseInitializer,
    BiConsumer<Database, Boolean> extendedStart) {
    if (db == null) {
      Settings settings = new MapSettings().addProperties(System.getProperties());
      loadOrchestratorSettings(settings);
      logJdbcSettings(settings);
      db = databaseCreator.apply(settings);
      db.start();

      databaseInitializer.accept(db);
      Loggers.get(getClass()).debug("Test Database: " + db);

      commands = DatabaseCommands.forDialect(db.getDialect());
      String login = settings.getString(JDBC_USERNAME.getKey());
      tester = new DataSourceDatabaseTester(db.getDataSource(), commands.useLoginAsSchema() ? login : null);

      extendedStart.accept(db, true);
    } else {
      extendedStart.accept(db, false);
    }
  }

  @Override
  public Database getDatabase() {
    return db;
  }

  @Override
  public DatabaseCommands getCommands() {
    return commands;
  }

  @Override
  public IDatabaseTester getDbUnitTester() {
    return tester;
  }

  @Override
  public void start() {
    if (!H2.ID.equals(db.getDialect().getId())) {
      throw new AssumptionViolatedException("Test disabled because it supports only H2");
    }
  }

  @Override
  public void stop() {
    db.stop();
  }

  private void logJdbcSettings(Settings settings) {
    Logger logger = Loggers.get(getClass());
    for (String key : settings.getKeysStartingWith("sonar.jdbc")) {
      logger.info(key + ": " + settings.getString(key));
    }
  }

  private static void loadOrchestratorSettings(Settings settings) {
    String url = settings.getString("orchestrator.configUrl");
    if (isEmpty(url)) {
      return;
    }

    InputStream input = null;
    try {
      URI uri = new URI(url);

      if (url.startsWith("file:")) {
        File file = new File(uri);
        input = FileUtils.openInputStream(file);
      } else {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        int responseCode = connection.getResponseCode();
        if (responseCode >= 400) {
          throw new IllegalStateException("Fail to request: " + uri + ". Status code=" + responseCode);
        }

        input = connection.getInputStream();
      }

      Properties props = new Properties();
      props.load(input);
      settings.addProperties(props);
      for (Map.Entry<String, String> entry : settings.getProperties().entrySet()) {
        String interpolatedValue = StrSubstitutor.replace(entry.getValue(), System.getenv(), "${", "}");
        settings.setProperty(entry.getKey(), interpolatedValue);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Cannot load Orchestrator properties from:" + url, e);
    } finally {
      IOUtils.closeQuietly(input);
    }
  }
}
