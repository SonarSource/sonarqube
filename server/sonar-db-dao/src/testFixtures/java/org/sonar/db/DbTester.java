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

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.alm.integration.pat.AlmPatsDbTester;
import org.sonar.db.almsettings.AlmSettingsDbTester;
import org.sonar.db.audit.AuditDbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.NoOpAuditPersister;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ProjectLinkDbTester;
import org.sonar.db.event.EventDbTester;
import org.sonar.db.favorite.FavoriteDbTester;
import org.sonar.db.issue.IssueDbTester;
import org.sonar.db.measure.MeasureDbTester;
import org.sonar.db.newcodeperiod.NewCodePeriodDbTester;
import org.sonar.db.notification.NotificationDbTester;
import org.sonar.db.permission.template.PermissionTemplateDbTester;
import org.sonar.db.plugin.PluginDbTester;
import org.sonar.db.property.InternalComponentPropertyDbTester;
import org.sonar.db.property.PropertyDbTester;
import org.sonar.db.qualitygate.QualityGateDbTester;
import org.sonar.db.qualityprofile.QualityProfileDbTester;
import org.sonar.db.rule.RuleDbTester;
import org.sonar.db.source.FileSourceTester;
import org.sonar.db.user.UserDbTester;
import org.sonar.db.webhook.WebhookDbTester;
import org.sonar.db.webhook.WebhookDeliveryDbTester;

/**
 * This class should be called using @Rule.
 * Data is truncated between each tests. The schema is created between each test.
 */
public class DbTester extends AbstractDbTester<TestDbImpl> {

  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();
  private final System2 system2;
  private final AuditPersister auditPersister;
  private DbClient client;
  ThreadLocal<DbSession> session = new ThreadLocal<>();
  private final UserDbTester userTester;
  private final ComponentDbTester componentTester;
  private final ProjectLinkDbTester componentLinkTester;
  private final FavoriteDbTester favoriteTester;
  private final EventDbTester eventTester;
  private final PermissionTemplateDbTester permissionTemplateTester;
  private final PropertyDbTester propertyTester;
  private final QualityGateDbTester qualityGateDbTester;
  private final IssueDbTester issueDbTester;
  private final RuleDbTester ruleDbTester;
  private final NewCodePeriodDbTester newCodePeriodTester;
  private final NotificationDbTester notificationDbTester;
  private final QualityProfileDbTester qualityProfileDbTester;
  private final MeasureDbTester measureDbTester;
  private final FileSourceTester fileSourceTester;
  private final PluginDbTester pluginDbTester;
  private final WebhookDbTester webhookDbTester;
  private final WebhookDeliveryDbTester webhookDeliveryDbTester;
  private final InternalComponentPropertyDbTester internalComponentPropertyTester;
  private final AlmSettingsDbTester almSettingsDbTester;
  private final AlmPatsDbTester almPatsDbtester;
  private final AuditDbTester auditDbTester;

  private DbTester(System2 system2, @Nullable String schemaPath, AuditPersister auditPersister, MyBatisConfExtension... confExtensions) {
    super(TestDbImpl.create(schemaPath, confExtensions));
    this.system2 = system2;
    this.auditPersister = auditPersister;

    initDbClient();
    this.userTester = new UserDbTester(this);
    this.componentTester = new ComponentDbTester(this);
    this.componentLinkTester = new ProjectLinkDbTester(this);
    this.favoriteTester = new FavoriteDbTester(this);
    this.eventTester = new EventDbTester(this);
    this.permissionTemplateTester = new PermissionTemplateDbTester(this);
    this.propertyTester = new PropertyDbTester(this);
    this.qualityGateDbTester = new QualityGateDbTester(this);
    this.issueDbTester = new IssueDbTester(this);
    this.ruleDbTester = new RuleDbTester(this);
    this.notificationDbTester = new NotificationDbTester(this);
    this.qualityProfileDbTester = new QualityProfileDbTester(this);
    this.measureDbTester = new MeasureDbTester(this);
    this.fileSourceTester = new FileSourceTester(this);
    this.pluginDbTester = new PluginDbTester(this);
    this.webhookDbTester = new WebhookDbTester(this);
    this.webhookDeliveryDbTester = new WebhookDeliveryDbTester(this);
    this.internalComponentPropertyTester = new InternalComponentPropertyDbTester(this);
    this.newCodePeriodTester = new NewCodePeriodDbTester(this);
    this.almSettingsDbTester = new AlmSettingsDbTester(this);
    this.almPatsDbtester = new AlmPatsDbTester(this);
    this.auditDbTester = new AuditDbTester(this);
  }

  public static DbTester create() {
    return new DbTester(System2.INSTANCE, null, new NoOpAuditPersister());
  }

  public static DbTester create(AuditPersister auditPersister) {
    return new DbTester(System2.INSTANCE, null, auditPersister);
  }

  public static DbTester create(System2 system2, AuditPersister auditPersister) {
    return new DbTester(system2, null, auditPersister);
  }

  public static DbTester create(System2 system2) {
    return new DbTester(system2, null, new NoOpAuditPersister());
  }

  public static DbTester createWithExtensionMappers(System2 system2, Class<?> firstMapperClass, Class<?>... otherMapperClasses) {
    return new DbTester(system2, null, new NoOpAuditPersister(), new DbTesterMyBatisConfExtension(firstMapperClass, otherMapperClasses));
  }

  private void initDbClient() {
    FastSpringContainer ioc = new FastSpringContainer();
    ioc.add(auditPersister);
    ioc.add(db.getMyBatis());
    ioc.add(system2);
    ioc.add(uuidFactory);
    for (Class<?> daoClass : DaoModule.classes()) {
      ioc.add(daoClass);
    }
    ioc.start();
    List<Dao> daos = ioc.getComponentsByType(Dao.class);
    client = new DbClient(db.getDatabase(), db.getMyBatis(), new TestDBSessions(db.getMyBatis()), daos.toArray(new Dao[daos.size()]));
  }

  @Override
  protected void before() {
    db.start();
    db.truncateTables();
  }

  public UserDbTester users() {
    return userTester;
  }

  public ComponentDbTester components() {
    return componentTester;
  }

  public ProjectLinkDbTester componentLinks() {
    return componentLinkTester;
  }

  public FavoriteDbTester favorites() {
    return favoriteTester;
  }

  public EventDbTester events() {
    return eventTester;
  }

  public PermissionTemplateDbTester permissionTemplates() {
    return permissionTemplateTester;
  }

  public PropertyDbTester properties() {
    return propertyTester;
  }

  public QualityGateDbTester qualityGates() {
    return qualityGateDbTester;
  }

  public IssueDbTester issues() {
    return issueDbTester;
  }

  public RuleDbTester rules() {
    return ruleDbTester;
  }

  public NewCodePeriodDbTester newCodePeriods() {
    return newCodePeriodTester;
  }

  public NotificationDbTester notifications() {
    return notificationDbTester;
  }

  public QualityProfileDbTester qualityProfiles() {
    return qualityProfileDbTester;
  }

  public MeasureDbTester measures() {
    return measureDbTester;
  }

  public FileSourceTester fileSources() {
    return fileSourceTester;
  }

  public PluginDbTester pluginDbTester() {
    return pluginDbTester;
  }

  public WebhookDbTester webhooks() {
    return webhookDbTester;
  }

  public WebhookDeliveryDbTester webhookDelivery() {
    return webhookDeliveryDbTester;
  }

  public InternalComponentPropertyDbTester internalComponentProperties() {
    return internalComponentPropertyTester;
  }

  public AlmSettingsDbTester almSettings() {
    return almSettingsDbTester;
  }

  public AlmPatsDbTester almPats() {
    return almPatsDbtester;
  }

  public AuditDbTester audits() {
    return auditDbTester;
  }

  @Override
  protected void after() {
    if (session.get() != null) {
      session.get().rollback();
      session.get().close();
      session.remove();
    }
    db.stop();
  }

  public DbSession getSession() {
    if (session.get() == null) {
      session.set(db.getMyBatis().openSession(false));
    }
    return session.get();
  }

  public void commit() {
    getSession().commit();
  }

  public DbClient getDbClient() {
    return client;
  }

  public int countRowsOfTable(DbSession dbSession, String tableName) {
    return super.countRowsOfTable(tableName, new DbSessionConnectionSupplier(dbSession));
  }

  public int countSql(DbSession dbSession, String sql) {
    return super.countSql(sql, new DbSessionConnectionSupplier(dbSession));
  }

  public List<Map<String, Object>> select(DbSession dbSession, String selectSql) {
    return super.select(selectSql, new DbSessionConnectionSupplier(dbSession));
  }

  public Map<String, Object> selectFirst(DbSession dbSession, String selectSql) {
    return super.selectFirst(selectSql, new DbSessionConnectionSupplier(dbSession));
  }

  @Deprecated
  public MyBatis myBatis() {
    return db.getMyBatis();
  }

  @Deprecated
  public Connection openConnection() throws SQLException {
    return getConnection();
  }

  private Connection getConnection() throws SQLException {
    return db.getDatabase().getDataSource().getConnection();
  }

  @Deprecated
  public Database database() {
    return db.getDatabase();
  }

  public String getUrl() {
    return ((HikariDataSource) db.getDatabase().getDataSource()).getJdbcUrl();
  }

  private static class DbSessionConnectionSupplier implements ConnectionSupplier {
    private final DbSession dbSession;

    public DbSessionConnectionSupplier(DbSession dbSession) {
      this.dbSession = dbSession;
    }

    @Override
    public Connection get() {
      return dbSession.getConnection();
    }

    @Override
    public void close() {
      // closing dbSession is not our responsibility
    }
  }

  private static class DbTesterMyBatisConfExtension implements MyBatisConfExtension {
    // do not replace with a lambda to allow cache of MyBatis instances in TestDbImpl to work
    private final Class<?>[] mapperClasses;

    public DbTesterMyBatisConfExtension(Class<?> firstMapperClass, Class<?>... otherMapperClasses) {
      this.mapperClasses = Stream.concat(
        Stream.of(firstMapperClass),
        Arrays.stream(otherMapperClasses))
        .sorted(Comparator.comparing(Class::getName))
        .toArray(Class<?>[]::new);
    }

    @Override
    public Stream<Class<?>> getMapperClasses() {
      return Arrays.stream(mapperClasses);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      DbTesterMyBatisConfExtension that = (DbTesterMyBatisConfExtension) o;
      return Arrays.equals(mapperClasses, that.mapperClasses);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(mapperClasses);
    }
  }
}
