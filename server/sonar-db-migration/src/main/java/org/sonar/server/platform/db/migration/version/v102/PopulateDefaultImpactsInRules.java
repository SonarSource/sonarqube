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
package org.sonar.server.platform.db.migration.version.v102;

import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.internal.ImpactMapper;
import org.sonar.core.util.Uuids;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;

public class PopulateDefaultImpactsInRules extends DataChange {

  private static final Logger LOG = LoggerFactory.getLogger(PopulateDefaultImpactsInRules.class);

  private static final String SELECT_COUNT_QUERY = """
    SELECT COUNT(*) FROM rules_default_impacts
    """;
  private static final String SELECT_QUERY = """
    SELECT r.uuid, rule_type, priority, ad_hoc_type, ad_hoc_severity, is_ad_hoc
    FROM rules r
    LEFT JOIN rules_default_impacts rdi ON rdi.rule_uuid = r.uuid
    WHERE rdi.uuid IS NULL
    """;

  private static final String INSERT_QUERY = """
    INSERT INTO rules_default_impacts (uuid, rule_uuid, software_quality, severity)
    VALUES (?, ?, ?, ?)
    """;

  public PopulateDefaultImpactsInRules(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    if (hasImpactsRecords(context)) {
      return;
    }
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(SELECT_QUERY);
    massUpdate.update(INSERT_QUERY);

    massUpdate.execute((row, update, index) -> {
      String ruleUuid = row.getString(1);
      String ruleType = row.getString(2);
      String severity = row.getString(3);
      String adhocType = row.getString(4);
      String adhocSeverity = row.getString(5);
      boolean isAdhoc = row.getBoolean(6);

      SoftwareQuality softwareQuality;
      Severity impactSeverity;

      try {
        RuleType effectiveType = null;
        String effectiveSeverity = null;
        if (isAdhoc && adhocType != null && adhocSeverity != null) {
          effectiveType = RuleType.valueOf(Integer.valueOf(adhocType));
          effectiveSeverity = adhocSeverity;
        } else if (!isAdhoc && ruleType != null && !ruleType.equals("0") && severity != null) {
          effectiveType = RuleType.valueOf(Integer.valueOf(ruleType));
          effectiveSeverity = org.sonar.api.rule.Severity.ALL.get(Integer.valueOf(severity));
        } else if (!isAdhoc) {
          //When type and severity are missing, we are in the case of a "placeholder" adhoc_rule that was created as default with an external issue.
          //In that case, we want to set default values for the impact. Otherwise, we don't populate the impact
          return false;
        }
        if (effectiveType == RuleType.SECURITY_HOTSPOT) {
          return false;
        }
        if (effectiveType != null && effectiveSeverity != null) {
          softwareQuality = ImpactMapper.convertToSoftwareQuality(effectiveType);
          impactSeverity = ImpactMapper.convertToImpactSeverity(effectiveSeverity);
        } else {
          softwareQuality = SoftwareQuality.MAINTAINABILITY;
          impactSeverity = Severity.MEDIUM;
        }
      } catch (Exception e) {
        LOG.warn("Error while mapping type to impact for rule '%s'".formatted(ruleUuid));
        LOG.debug("Error while mapping type to impact for rule '%s'".formatted(ruleUuid), e);
        return false;
      }

      update.setString(1, Uuids.create())
        .setString(2, ruleUuid)
        .setString(3, softwareQuality.name())
        .setString(4, impactSeverity.name());
      return true;
    });
  }

  private static boolean hasImpactsRecords(Context context) throws SQLException {
    Long recordNumber = context.prepareSelect(SELECT_COUNT_QUERY).get(Select.LONG_READER);
    return recordNumber != null && recordNumber > 0;
  }
}
