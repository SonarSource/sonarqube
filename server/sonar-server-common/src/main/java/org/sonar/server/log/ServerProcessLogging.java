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
package org.sonar.server.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.Encoder;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sonar.process.ProcessId;
import org.sonar.process.Props;
import org.sonar.process.logging.LogLevelConfig;
import org.sonar.process.logging.LogbackHelper;
import org.sonar.process.logging.RootLoggerConfig;

import static org.sonar.process.logging.RootLoggerConfig.newRootLoggerConfigBuilder;

public abstract class ServerProcessLogging {

  public static final String STARTUP_LOGGER_NAME = "startup";
  protected static final Set<String> JMX_RMI_LOGGER_NAMES = ImmutableSet.of(
    "javax.management.remote.timeout",
    "javax.management.remote.misc",
    "javax.management.remote.rmi",
    "javax.management.mbeanserver",
    "sun.rmi.loader",
    "sun.rmi.transport.tcp",
    "sun.rmi.transport.misc",
    "sun.rmi.server.call",
    "sun.rmi.dgc");
  protected static final Set<String> LOGGER_NAMES_TO_TURN_OFF = ImmutableSet.of(
    // mssql driver
    "com.microsoft.sqlserver.jdbc.internals",
    "com.microsoft.sqlserver.jdbc.ResultSet",
    "com.microsoft.sqlserver.jdbc.Statement",
    "com.microsoft.sqlserver.jdbc.Connection");

  private final ProcessId processId;
  private final String threadIdFieldPattern;
  private final LogbackHelper helper = new LogbackHelper();
  private final LogLevelConfig logLevelConfig;

  protected ServerProcessLogging(ProcessId processId, String threadIdFieldPattern) {
    this.processId = processId;
    this.threadIdFieldPattern = threadIdFieldPattern;
    this.logLevelConfig = createLogLevelConfiguration(processId);
  }

  private LogLevelConfig createLogLevelConfiguration(ProcessId processId) {
    LogLevelConfig.Builder builder = LogLevelConfig.newBuilder(helper.getRootLoggerName());
    builder.rootLevelFor(processId);
    builder.immutableLevel("org.apache.ibatis", Level.WARN);
    builder.immutableLevel("java.sql", Level.WARN);
    builder.immutableLevel("java.sql.ResultSet", Level.WARN);
    builder.immutableLevel("org.elasticsearch", Level.INFO);
    builder.immutableLevel("org.elasticsearch.node", Level.INFO);
    builder.immutableLevel("org.elasticsearch.http", Level.INFO);
    builder.immutableLevel("ch.qos.logback", Level.WARN);
    builder.immutableLevel("org.apache.catalina", Level.INFO);
    builder.immutableLevel("org.apache.coyote", Level.INFO);
    builder.immutableLevel("org.apache.jasper", Level.INFO);
    builder.immutableLevel("org.apache.tomcat", Level.INFO);
    builder.immutableLevel("org.postgresql.core.v3.QueryExecutorImpl", Level.INFO);
    builder.immutableLevel("org.postgresql.jdbc.PgConnection", Level.INFO);
    // Apache FOP
    builder.immutableLevel("org.apache.fop", Level.INFO);
    builder.immutableLevel("org.apache.fop.apps.FOUserAgent", Level.WARN);
    builder.immutableLevel("org.apache.xmlgraphics.image.loader.spi.ImageImplRegistry", Level.INFO);
    // Hazelcast
    builder.immutableLevel("com.hazelcast.internal.cluster.impl.ClusterHeartbeatManager", Level.INFO);
    builder.immutableLevel("com.hazelcast.internal.cluster.impl.operations.HeartbeatOperation", Level.INFO);
    builder.immutableLevel("com.hazelcast.internal.partition.InternalPartitionService", Level.INFO);
    builder.immutableLevel("com.hazelcast.internal.partition.operation.PartitionStateOperation", Level.INFO);
    builder.immutableLevel("com.hazelcast.replicatedmap.impl.operation.RequestMapDataOperation", Level.INFO);
    builder.immutableLevel("com.hazelcast.replicatedmap.impl.operation.SyncReplicatedMapDataOperation", Level.INFO);
    // Netty (used by Elasticsearch)
    builder.immutableLevel("io.netty.buffer.PoolThreadCache", Level.INFO);

    extendLogLevelConfiguration(builder);

    return builder.build();
  }

  public LoggerContext configure(Props props) {
    LoggerContext ctx = helper.getRootContext();
    ctx.reset();

    configureRootLogger(props);
    helper.apply(logLevelConfig, props);
    configureDirectToConsoleLoggers(props, ctx, STARTUP_LOGGER_NAME);
    extendConfigure();

    helper.enableJulChangePropagation(ctx);

    return ctx;
  }

  public LogLevelConfig getLogLevelConfig() {
    return this.logLevelConfig;
  }

  protected abstract void extendLogLevelConfiguration(LogLevelConfig.Builder logLevelConfigBuilder);

  protected abstract void extendConfigure();

  private void configureRootLogger(Props props) {
    RootLoggerConfig config = newRootLoggerConfigBuilder()
      .setProcessId(processId)
      .setThreadIdFieldPattern(threadIdFieldPattern)
      .build();
    Encoder<ILoggingEvent> encoder = helper.createEncoder(props, config, helper.getRootContext());
    helper.configureGlobalFileLog(props, config, encoder);
    helper.configureForSubprocessGobbler(props, encoder);
  }

  /**
   * Setup one or more specified loggers to be non additive and to print to System.out which will be caught by the Main
   * Process and written to sonar.log.
   */
  private void configureDirectToConsoleLoggers(Props props, LoggerContext context, String... loggerNames) {
    RootLoggerConfig config = newRootLoggerConfigBuilder()
      .setProcessId(ProcessId.APP)
      .setThreadIdFieldPattern("")
      .build();
    Encoder<ILoggingEvent> encoder = helper.createEncoder(props, config, context);
    ConsoleAppender<ILoggingEvent> consoleAppender = helper.newConsoleAppender(context, "CONSOLE", encoder);

    for (String loggerName : loggerNames) {
      Logger consoleLogger = context.getLogger(loggerName);
      consoleLogger.setAdditive(false);
      consoleLogger.addAppender(consoleAppender);
    }
  }
}
