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
package org.sonar.server.platform.db.migration.version.v73;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.sql.SQLException;
import java.util.List;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.MySql;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

import static java.util.stream.Collectors.joining;

@SupportsBlueGreen
// SONAR-11021
public class FixMissingQualityProfilesOnOrganizations extends DataChange {

  private final System2 system2;
  private final UuidFactory uuidFactory;
  private final Configuration configuration;
  private final String as;

  public FixMissingQualityProfilesOnOrganizations(Database db, System2 system2, UuidFactory uuidFactory, Configuration configuration) {
    super(db);
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
    this.configuration = configuration;
    if (db.getDialect().getId().equals(MySql.ID) || db.getDialect().getId().equals(MsSql.ID)) {
      as = " AS ";
    } else {
      as = "";
    }
  }

  @Override
  protected void execute(Context context) throws SQLException {
    if (!configuration.getBoolean("sonar.sonarcloud.enabled").orElse(false)) {
      return;
    }

    long now = system2.now();

    insertMissingOrgQProfiles(context, now);
    insertMissingDefaultQProfiles(context, now);
  }

  private void insertMissingOrgQProfiles(Context context, long now) throws SQLException {
    MassUpdate massUpdate = context
      .prepareMassUpdate()
      .rowPluralName("Organization quality profiles");
    massUpdate.select("SELECT o.uuid, rp.kee FROM organizations " + as + " o, rules_profiles " + as + " rp " +
      "WHERE rp.is_built_in = ? " +
      "AND NOT EXISTS(SELECT (1) FROM org_qprofiles oqp WHERE oqp.organization_uuid = o.uuid AND oqp.rules_profile_uuid = rp.kee)")
      .setBoolean(1, true);
    massUpdate.update("INSERT INTO org_qprofiles (uuid, organization_uuid, rules_profile_uuid, created_at, updated_at) VALUES(?, ?, ?, ?, ?)");
    massUpdate.execute((row, update) -> {
      String organizationUuid = row.getString(1);
      String rulesProfileUuid = row.getString(2);
      update.setString(1, uuidFactory.create());
      update.setString(2, organizationUuid);
      update.setString(3, rulesProfileUuid);
      update.setLong(4, now);
      update.setLong(5, now);

      return true;
    });
  }

  private void insertMissingDefaultQProfiles(Context context, long now) throws SQLException {
    String defaultRulesProfileKees = reduceBuiltInQualityProfiles(context)
      .stream()
      .map(qp -> "'" + qp.kee + "'")
      .collect(joining(","));

    if (defaultRulesProfileKees.isEmpty()) {
      return;
    }

    MassUpdate massUpdate = context
      .prepareMassUpdate()
      .rowPluralName("Organization default quality profiles");
    massUpdate.select("SELECT o.uuid, oqp.uuid, rp.language FROM organizations " + as + " o, org_qprofiles " + as + " oqp, rules_profiles " + as + " rp " +
      "WHERE oqp.rules_profile_uuid = rp.kee " +
      "AND oqp.organization_uuid = o.uuid " +
      "AND rp.kee IN ( " + defaultRulesProfileKees + " ) " +
      "AND NOT EXISTS(SELECT(1) FROM default_qprofiles dqp WHERE dqp.organization_uuid = o.uuid AND dqp.language = rp.language)");
    massUpdate.update("INSERT INTO default_qprofiles (organization_uuid, language, qprofile_uuid, created_at, updated_at) VALUES(?, ?, ?, ?, ?)");
    massUpdate.execute((row, update) -> {
      String organizationUuid = row.getString(1);
      String orgQProfileUuid = row.getString(2);
      String language = row.getString(3);
      update.setString(1, organizationUuid);
      update.setString(2, language);
      update.setString(3, orgQProfileUuid);
      update.setLong(4, now);
      update.setLong(5, now);

      return true;
    });
  }

  /**
   * Return the list of preferred built-in quality profiles.
   * In the current state of database, the information of the "by-default" one is absent (handled by plugin).
   * Let's choose the "Sonar Way" one, fallbacking to the first one
   *
   * This methods is returning the list of rules_profiles kee of built-in quality profiles with one by language
   */
  private static List<BuiltInQProfile> reduceBuiltInQualityProfiles(Context context) throws SQLException {
    ListMultimap<String, BuiltInQProfile> builtInQPByLanguages = ArrayListMultimap.create();

    List<BuiltInQProfile> builtInQProfiles = context.prepareSelect("SELECT kee, language, name FROM rules_profiles WHERE is_built_in = ?")
      .setBoolean(1, true)
      .list(row -> new BuiltInQProfile(row.getString(1), row.getString(2), row.getString(3)));

    builtInQProfiles.forEach(builtInQProfile -> builtInQPByLanguages.put(builtInQProfile.language, builtInQProfile));

    // Filter all built in quality profiles to have only one by language
    // And prefer the one named "Sonar Way"
    builtInQPByLanguages.keySet().forEach(l -> {
      List<BuiltInQProfile> qps = builtInQPByLanguages.get(l);
      if (qps.size() > 1) {
        BuiltInQProfile sonarWay = qps.stream().filter(qp -> qp.name.equals("Sonar way"))
          .findFirst()
          .orElse(qps.get(0));
        qps.forEach(qp -> {
          if (qp.kee.equals(sonarWay.kee)) {
            return;
          }
          builtInQProfiles.remove(qp);
        });
      }
    });

    return builtInQProfiles;
  }

  private static class BuiltInQProfile {
    private final String kee;
    private final String language;
    private final String name;

    public BuiltInQProfile(String kee, String language, String name) {
      this.kee = kee;
      this.language = language;
      this.name = name;
    }
  }
}
