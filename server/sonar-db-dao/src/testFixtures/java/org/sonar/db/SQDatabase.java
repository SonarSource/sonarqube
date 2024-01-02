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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.apache.commons.io.output.NullWriter;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.Container;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.core.platform.SpringComponentContainer;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.core.util.logs.Profiler;
import org.sonar.db.dialect.Dialect;
import org.sonar.process.logging.LogbackHelper;
import org.sonar.server.platform.db.migration.MigrationConfigurationModule;
import org.sonar.server.platform.db.migration.engine.MigrationContainer;
import org.sonar.server.platform.db.migration.engine.MigrationContainerImpl;
import org.sonar.server.platform.db.migration.es.MigrationEsClient;
import org.sonar.server.platform.db.migration.history.MigrationHistoryTableImpl;
import org.sonar.server.platform.db.migration.step.MigrationStep;
import org.sonar.server.platform.db.migration.step.MigrationStepExecutionException;
import org.sonar.server.platform.db.migration.step.MigrationSteps;
import org.sonar.server.platform.db.migration.step.MigrationStepsExecutor;
import org.sonar.server.platform.db.migration.step.RegisteredMigrationStep;

import static com.google.common.base.Preconditions.checkState;

public class SQDatabase extends DefaultDatabase {
  private static final String IGNORED_KEYWORDS_OPTION = ";NON_KEYWORDS=VALUE";
  private final boolean createSchema;

  private SQDatabase(Settings settings, boolean createSchema) {
    super(new LogbackHelper(), settings);
    this.createSchema = createSchema;
  }

  public static SQDatabase newDatabase(Settings settings, boolean createSchema) {
    return new SQDatabase(settings, createSchema);
  }

  public static SQDatabase newH2Database(String name, boolean createSchema) {
    MapSettings settings = new MapSettings()
      .setProperty("sonar.jdbc.dialect", "h2")
      .setProperty("sonar.jdbc.driverClassName", "org.h2.Driver")
      .setProperty("sonar.jdbc.url", "jdbc:h2:mem:" + name + IGNORED_KEYWORDS_OPTION)
      .setProperty("sonar.jdbc.username", "sonar")
      .setProperty("sonar.jdbc.password", "sonar");
    return new SQDatabase(settings, createSchema);
  }

  @Override
  public void start() {
    super.start();
    if (createSchema) {
      createSchema();
    }
  }

  private void createSchema() {
    Connection connection = null;
    try {
      connection = getDataSource().getConnection();
      NoopDatabase noopDatabase = new NoopDatabase(getDialect(), getDataSource());
      // create and populate schema
      createMigrationHistoryTable(noopDatabase);
      executeDbMigrations(noopDatabase);
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to create schema", e);
    } finally {
      DatabaseUtils.closeQuietly(connection);
    }
  }

  public static final class H2StepExecutor implements MigrationStepsExecutor {
    private static final String STEP_START_PATTERN = "{}...";
    private static final String STEP_STOP_PATTERN = "{}: {}";

    private final Container container;

    public H2StepExecutor(Container container) {
      this.container = container;
    }

    @Override
    public void execute(List<RegisteredMigrationStep> steps) {
      steps.forEach(step -> execute(step, container));
    }

    private void execute(RegisteredMigrationStep step, Container container) {
      MigrationStep migrationStep = container.getComponentByType(step.getStepClass());
      checkState(migrationStep != null, "Can not find instance of " + step.getStepClass());

      execute(step, migrationStep);
    }

    private void execute(RegisteredMigrationStep step, MigrationStep migrationStep) {
      Profiler stepProfiler = Profiler.create(Loggers.get(SQDatabase.class));
      stepProfiler.startInfo(STEP_START_PATTERN, step);
      boolean done = false;
      try {
        migrationStep.execute();
        done = true;
      } catch (Exception e) {
        throw new MigrationStepExecutionException(step, e);
      } finally {
        if (done) {
          stepProfiler.stopInfo(STEP_STOP_PATTERN, step, "success");
        } else {
          stepProfiler.stopError(STEP_STOP_PATTERN, step, "failure");
        }
      }
    }
  }

  private void executeDbMigrations(NoopDatabase noopDatabase) {
    SpringComponentContainer container = new SpringComponentContainer();
    container.add(noopDatabase);
    MigrationConfigurationModule migrationConfigurationModule = new MigrationConfigurationModule();
    migrationConfigurationModule.configure(container);

    // dependencies required by DB migrations
    container.add(new SonarQubeVersion(Version.create(8, 0)));
    container.add(UuidFactoryFast.getInstance());
    container.add(System2.INSTANCE);
    container.add(MapSettings.class);
    container.add(createMockMigrationEsClient());

    container.startComponents();
    MigrationContainer migrationContainer = new MigrationContainerImpl(container, H2StepExecutor.class);
    MigrationSteps migrationSteps = migrationContainer.getComponentByType(MigrationSteps.class);
    MigrationStepsExecutor executor = migrationContainer.getComponentByType(MigrationStepsExecutor.class);

    executor.execute(migrationSteps.readAll());
  }

  private static MigrationEsClient createMockMigrationEsClient() {
    return new MigrationEsClient() {
      @Override
      public void deleteIndexes(String name, String... otherNames) {
        //No ES operation required for database tests
      }

      @Override
      public void addMappingToExistingIndex(String index, String type, String mappingName, String mappingType, Map<String, String> options) {
        //No ES operation required for database tests
      }

      @Override
      public Set<String> getUpdatedIndices() {
        return Collections.emptySet();
      }
    };
  }

  private void createMigrationHistoryTable(NoopDatabase noopDatabase) {
    new MigrationHistoryTableImpl(noopDatabase).start();
  }

  private static class NoopDatabase implements Database {
    private final Dialect dialect;
    private final DataSource dataSource;

    private NoopDatabase(Dialect dialect, DataSource dataSource) {
      this.dialect = dialect;
      this.dataSource = dataSource;
    }

    @Override
    public DataSource getDataSource() {
      return dataSource;
    }

    @Override
    public Dialect getDialect() {
      return dialect;
    }

    @Override
    public void enableSqlLogging(boolean enable) {

    }

    @Override
    public void start() {
      // do nothing
    }

    @Override
    public void stop() {
      // do nothing
    }
  }

  public void executeScript(String classloaderPath) {
    try (Connection connection = getDataSource().getConnection()) {
      executeScript(connection, classloaderPath);
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to execute script: " + classloaderPath, e);
    }
  }

  private static void executeScript(Connection connection, String path) {
    ScriptRunner scriptRunner = newScriptRunner(connection);
    try {
      scriptRunner.runScript(Resources.getResourceAsReader(path));
      connection.commit();

    } catch (Exception e) {
      throw new IllegalStateException("Fail to restore: " + path, e);
    }
  }

  private static ScriptRunner newScriptRunner(Connection connection) {
    ScriptRunner scriptRunner = new ScriptRunner(connection);
    scriptRunner.setDelimiter(";");
    scriptRunner.setStopOnError(true);
    scriptRunner.setLogWriter(new PrintWriter(new NullWriter()));
    return scriptRunner;
  }
}
