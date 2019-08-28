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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.io.output.NullWriter;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.core.util.logs.Profiler;
import org.sonar.db.dialect.Dialect;
import org.sonar.process.logging.LogbackHelper;
import org.sonar.server.platform.db.migration.MigrationConfigurationModule;
import org.sonar.server.platform.db.migration.engine.MigrationContainer;
import org.sonar.server.platform.db.migration.engine.MigrationContainerImpl;
import org.sonar.server.platform.db.migration.engine.MigrationContainerPopulator;
import org.sonar.server.platform.db.migration.engine.MigrationContainerPopulatorImpl;
import org.sonar.server.platform.db.migration.history.MigrationHistoryTableImpl;
import org.sonar.server.platform.db.migration.step.MigrationStep;
import org.sonar.server.platform.db.migration.step.MigrationStepExecutionException;
import org.sonar.server.platform.db.migration.step.MigrationSteps;
import org.sonar.server.platform.db.migration.step.MigrationStepsExecutor;
import org.sonar.server.platform.db.migration.step.RegisteredMigrationStep;
import org.sonar.server.platform.db.migration.version.DbVersion;

import static com.google.common.base.Preconditions.checkState;

public class SQDatabase extends DefaultDatabase {
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
      .setProperty("sonar.jdbc.url", "jdbc:h2:mem:" + name)
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
      DbUtils.closeQuietly(connection);
    }
  }

  public static final class H2MigrationContainerPopulator extends MigrationContainerPopulatorImpl {
    public H2MigrationContainerPopulator(DbVersion... dbVersions) {
      super(H2StepExecutor.class, dbVersions);
    }
  }

  public static final class H2StepExecutor implements MigrationStepsExecutor {
    private static final String STEP_START_PATTERN = "{}...";
    private static final String STEP_STOP_PATTERN = "{}: {}";

    private final ComponentContainer componentContainer;

    public H2StepExecutor(ComponentContainer componentContainer) {
      this.componentContainer = componentContainer;
    }

    @Override
    public void execute(List<RegisteredMigrationStep> steps) {
      steps.forEach(step -> execute(step, componentContainer));
    }

    private void execute(RegisteredMigrationStep step, ComponentContainer componentContainer) {
      MigrationStep migrationStep = componentContainer.getComponentByType(step.getStepClass());
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
    ComponentContainer parentContainer = new ComponentContainer();
    parentContainer.add(noopDatabase);
    parentContainer.add(H2MigrationContainerPopulator.class);
    MigrationConfigurationModule migrationConfigurationModule = new MigrationConfigurationModule();
    migrationConfigurationModule.configure(parentContainer);

    // dependencies required by DB migrations
    parentContainer.add(SonarRuntimeImpl.forSonarQube(Version.create(8, 0), SonarQubeSide.SERVER, SonarEdition.COMMUNITY));
    parentContainer.add(UuidFactoryFast.getInstance());
    parentContainer.add(System2.INSTANCE);

    parentContainer.startComponents();

    MigrationContainer migrationContainer = new MigrationContainerImpl(parentContainer, parentContainer.getComponentByType(MigrationContainerPopulator.class));
    MigrationSteps migrationSteps = migrationContainer.getComponentByType(MigrationSteps.class);
    migrationContainer.getComponentByType(MigrationStepsExecutor.class)
      .execute(migrationSteps.readAll());
  }

  private void createMigrationHistoryTable(NoopDatabase noopDatabase) {
    new MigrationHistoryTableImpl(noopDatabase).start();
  }

  private class NoopDatabase implements Database {
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
