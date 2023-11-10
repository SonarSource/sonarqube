/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import org.apache.commons.io.output.NullWriter;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
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
  @Nullable
  private final Class<? extends MigrationStep> migrationStep;

  private SQDatabase(Settings settings, boolean createSchema, @Nullable Class<? extends MigrationStep> migrationStep) {
    super(new LogbackHelper(), settings);
    this.createSchema = createSchema;
    this.migrationStep = migrationStep;
  }

  public static final class Builder {
    String h2Name;

    Settings settings;

    boolean createSchema = false;

    Class<? extends MigrationStep> migrationStep;

    public Builder withSettings(Settings settings) {
      this.settings = settings;
      return this;
    }

    public Builder untilMigrationStep(Class<? extends MigrationStep> migrationStep) {
      this.migrationStep = migrationStep;
      return this;
    }

    public Builder createSchema(boolean createSchema) {
      this.createSchema = createSchema;
      return this;
    }

    public Builder asH2Database(String h2Name) {
      this.h2Name = h2Name;
      return this;
    }

    public SQDatabase build() {
      checkState(migrationStep == null || createSchema, "Schema needs to be created to specify migration step");
      checkState(h2Name == null || settings == null, "Settings should not be specified for h2 database");
      return new SQDatabase(h2Name != null ? getSettingsForH2(h2Name) : settings, createSchema, migrationStep);
    }
  }

  private static MapSettings getSettingsForH2(String name) {
    return new MapSettings()
      .setProperty("sonar.jdbc.dialect", "h2")
      .setProperty("sonar.jdbc.driverClassName", "org.h2.Driver")
      .setProperty("sonar.jdbc.url", "jdbc:h2:mem:" + name + IGNORED_KEYWORDS_OPTION)
      .setProperty("sonar.jdbc.username", "sonar")
      .setProperty("sonar.jdbc.password", "sonar");
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
      executeDbMigrations(noopDatabase, migrationStep);
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
      Profiler stepProfiler = Profiler.create(LoggerFactory.getLogger(SQDatabase.class));
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

  private static void executeDbMigrations(NoopDatabase noopDatabase, @Nullable Class<? extends MigrationStep> stepClass) {
    SpringComponentContainer container = new SpringComponentContainer();
    container.add(noopDatabase);
    MigrationConfigurationModule migrationConfigurationModule = new MigrationConfigurationModule();
    migrationConfigurationModule.configure(container);

    // dependencies required by DB migrations
    container.add(new SonarQubeVersion(Version.create(8, 0)));
    container.add(UuidFactoryFast.getInstance());
    container.add(System2.INSTANCE);
    container.add(MapSettings.class);

    container.startComponents();
    MigrationContainer migrationContainer = new MigrationContainerImpl(container, H2StepExecutor.class);
    MigrationSteps migrationSteps = migrationContainer.getComponentByType(MigrationSteps.class);
    MigrationStepsExecutor executor = migrationContainer.getComponentByType(MigrationStepsExecutor.class);

    List<RegisteredMigrationStep> steps = migrationSteps.readAll();
    if (stepClass != null) {
      steps = filterUntilStep(steps, stepClass);
    }
    executor.execute(steps);
  }

  private static List<RegisteredMigrationStep> filterUntilStep(List<RegisteredMigrationStep> steps, Class<? extends MigrationStep> stepClass) {
    List<RegisteredMigrationStep> filteredSteps = new ArrayList<>();
    boolean found = false;
    for (RegisteredMigrationStep step : steps) {
      if (step.getStepClass().equals(stepClass)) {
        if (!found) {
          found = true;
        } else {
          throw new IllegalArgumentException("Duplicate step " + stepClass + " defined in migration");
        }
      }
      if (!found) {
        filteredSteps.add(step);
      }
    }
    if (!found) {
      throw new IllegalArgumentException(stepClass + " not found in the migration steps");
    }
    return filteredSteps;
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
