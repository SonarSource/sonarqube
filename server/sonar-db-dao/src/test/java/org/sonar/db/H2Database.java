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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.apache.commons.dbutils.DbUtils;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.core.util.logs.Profiler;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
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

/**
 * H2 in-memory database, used for unit tests against an empty DB, a specific script or against SQ schema.
 */
public class H2Database extends CoreH2Database {
  private final boolean createSchema;

  /**
   * IMPORTANT: change DB name in order to not conflict with {@link DefaultDatabaseTest}
   */
  public H2Database(String name, boolean createSchema) {
    super(name);
    this.createSchema = createSchema;
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
      NoopH2Database noopH2Database = new NoopH2Database();
      // create and populate schema
      createMigrationHistoryTable(noopH2Database);
      executeDbMigrations(noopH2Database);
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
      Profiler stepProfiler = Profiler.create(Loggers.get(H2Database.class));
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

  private void executeDbMigrations(NoopH2Database noopH2Database) {
    ComponentContainer parentContainer = new ComponentContainer();
    parentContainer.add(noopH2Database);
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

  private void createMigrationHistoryTable(NoopH2Database noopH2Database) {
    new MigrationHistoryTableImpl(noopH2Database).start();
  }

  private class NoopH2Database implements Database {
    @Override
    public DataSource getDataSource() {
      return H2Database.this.getDataSource();
    }

    @Override
    public Dialect getDialect() {
      return new H2();
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
}
