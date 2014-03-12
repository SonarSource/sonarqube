/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.startup;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.Database;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.technicaldebt.db.RequirementDao;
import org.sonar.core.technicaldebt.db.RequirementDto;
import org.sonar.server.db.migrations.MassUpdater;
import org.sonar.server.db.migrations.SqlUtil;
import org.sonar.server.rule.RuleRegistration;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.sql.*;
import java.util.Collection;
import java.util.List;

/**
 * This script need to be executed after rules registration because default debt columns (characteristics, function, factor and offset) has to be populated
 * in order to be able to compare default values with overridden values.
 *
 * @since 4.3 this component could be removed after 4 or 5 releases.
 */
public class CopyRequirementsFromCharacteristicsToRules {

  private static final Logger LOGGER = LoggerFactory.getLogger(CopyRequirementsFromCharacteristicsToRules.class);

  private final System2 system2;

  private final Database db;

  private final ServerUpgradeStatus status;

  private final RequirementDao requirementDao;

  /**
   * @param ruleRegistration used only to be started after init of rules
   */
  public CopyRequirementsFromCharacteristicsToRules(Database database, RequirementDao requirementDao, ServerUpgradeStatus status, RuleRegistration ruleRegistration) {
    this(database, requirementDao, status, System2.INSTANCE);
  }

  @VisibleForTesting
  CopyRequirementsFromCharacteristicsToRules(Database database, RequirementDao requirementDao, ServerUpgradeStatus status, System2 system2) {
    this.db = database;
    this.system2 = system2;
    this.status = status;
    this.requirementDao = requirementDao;
  }

  public void start() {
    if (mustDoPurge()) {
      doPurge();
    }
  }

  private boolean mustDoPurge() {
    return status.isUpgraded() && status.getInitialDbVersion() <= 520;
  }

  private void doPurge() {
    LOGGER.info("Copying requirement from characteristics to rules");
    copyRequirementsFromCharacteristicsToRules();

    LOGGER.info("Deleting requirements data");
    removeRequirementsDataFromCharacteristics();
  }

  private void copyRequirementsFromCharacteristicsToRules() {
    List<RequirementDto> requirementDtos = requirementDao.selectRequirements();
    final Multimap<Integer, RequirementDto> requirementsByRuleId = ArrayListMultimap.create();
    for (RequirementDto requirementDto : requirementDtos) {
      requirementsByRuleId.put(requirementDto.getRuleId(), requirementDto);
    }

    new MassUpdater(db).execute(
      new MassUpdater.InputLoader<Row>() {
        @Override
        public String selectSql() {
          return "SELECT r.id,r.characteristic_id,r.remediation_function,r.remediation_factor,r.remediation_offset," +
            "r.default_characteristic_id,r.default_remediation_function,r.default_remediation_factor,r.default_remediation_offset,r.status " +
            "FROM rules r";
        }

        @Override
        public Row load(ResultSet rs) throws SQLException {
          Row row = new Row();
          row.id = SqlUtil.getInt(rs, 1);
          row.characteristicId = SqlUtil.getInt(rs, 2);
          row.function = rs.getString(3);
          row.factor = rs.getString(4);
          row.offset = rs.getString(5);
          row.defaultCharacteristicId = SqlUtil.getInt(rs, 6);
          row.defaultFunction = rs.getString(7);
          row.defaultFactor = rs.getString(8);
          row.defaultOffset = rs.getString(9);
          row.status = rs.getString(10);
          return row;
        }
      },
      new MassUpdater.InputConverter<Row>() {
        @Override
        public String updateSql() {
          return "UPDATE rules SET characteristic_id=?,remediation_function=?,remediation_factor=?,remediation_offset=?,updated_at=? WHERE id=?";
        }

        @Override
        public boolean convert(Row row, PreparedStatement updateStatement) throws SQLException {
          Collection<RequirementDto> requirementsForCurrentRule = requirementsByRuleId.get(row.id);

          if (requirementsForCurrentRule.isEmpty()) {
            // Nothing to do, there's no requirement on this rule
            return false;

          } else {
            RequirementDto enabledRequirement = Iterables.find(requirementsForCurrentRule, new Predicate<RequirementDto>() {
              @Override
              public boolean apply(RequirementDto input) {
                return input.isEnabled();
              }
            }, null);

            if (enabledRequirement == null && !"REMOVED".equals(row.getStatus())) {
              // If no requirements are enable, it means that the requirement has been disabled for this rule
              updateStatement.setInt(1, RuleDto.DISABLED_CHARACTERISTIC_ID);
              updateStatement.setNull(2, Types.VARCHAR);
              updateStatement.setNull(3, Types.VARCHAR);
              updateStatement.setNull(4, Types.VARCHAR);
              updateStatement.setDate(5, new Date(system2.now()));
              updateStatement.setInt(6, row.getId());
              return true;

            } else if (enabledRequirement != null) {
              // If one requirement is enable, it means either that this requirement has been set from SQALE, or that it come from a XML model definition

              row.setCharacteristicId(enabledRequirement.getParentId());
              row.setFunction(enabledRequirement.getFunction().toUpperCase());
              row.setFactor(convertDuration(enabledRequirement.getFactorValue(), enabledRequirement.getFactorUnit()));
              row.setOffset(convertDuration(enabledRequirement.getOffsetValue(), enabledRequirement.getOffsetUnit()));

              if (isDebtDefaultValuesSameAsOverriddenValues(row)) {
                // Default values on debt are the same that ones set by SQALE, nothing to do
                return false;
              } else {
                // Default values on debt are not the same that ones set by SQALE, update the rule
                updateStatement.setInt(1, row.getCharacteristicId());
                updateStatement.setString(2, row.getFunction());
                updateStatement.setString(3, row.getFactor());
                updateStatement.setString(4, row.getOffset());
                updateStatement.setDate(5, new Date(system2.now()));
                updateStatement.setInt(6, row.getId());
                return true;
              }
            }
          }
          return false;
        }
      }
    );
  }

  @CheckForNull
  @VisibleForTesting
  String convertDuration(@Nullable Double oldValue, @Nullable String oldUnit) {
    if (oldValue != null) {
      String unit = oldUnit != null ? oldUnit : RequirementDto.DAYS;
      // min is replaced by mn
      unit = RequirementDto.MINUTES.equals(unit) ? Duration.MINUTE : unit;
      // As value is stored in double, we have to round it in order to have an integer (for instance, if it was 1.6, we'll use 2)
      return Integer.toString((int) Math.round(oldValue)) + unit;
    }
    return null;
  }

  @VisibleForTesting
  boolean isDebtDefaultValuesSameAsOverriddenValues(Row row) {
    return new EqualsBuilder()
      .append(row.getDefaultCharacteristicId(), row.getCharacteristicId())
      .append(row.getDefaultFunction(), row.getFunction())
      .append(row.getDefaultFactor(), row.getFactor())
      .append(row.getDefaultOffset(), row.getOffset())
      .isEquals();
  }


  private void removeRequirementsDataFromCharacteristics(){
    try {
      Connection connection = db.getDataSource().getConnection();
      Statement stmt = connection.createStatement();
      stmt.executeUpdate("DELETE FROM characteristics WHERE rule_id IS NOT NULL");
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to remove requirements data from characteristics");
    }
  }

  @VisibleForTesting
  static class Row {
    private Integer id;
    private Integer characteristicId;
    private Integer defaultCharacteristicId;
    private String function;
    private String defaultFunction;
    private String factor;
    private String defaultFactor;
    private String offset;
    private String defaultOffset;
    private String status;

    public Integer getId() {
      return id;
    }

    public Row setId(Integer id) {
      this.id = id;
      return this;
    }

    public Integer getCharacteristicId() {
      return characteristicId;
    }

    public Row setCharacteristicId(Integer characteristicId) {
      this.characteristicId = characteristicId;
      return this;
    }

    public Integer getDefaultCharacteristicId() {
      return defaultCharacteristicId;
    }

    public Row setDefaultCharacteristicId(Integer defaultCharacteristicId) {
      this.defaultCharacteristicId = defaultCharacteristicId;
      return this;
    }

    public String getFunction() {
      return function;
    }

    public Row setFunction(String function) {
      this.function = function;
      return this;
    }

    public String getDefaultFunction() {
      return defaultFunction;
    }

    public Row setDefaultFunction(String defaultFunction) {
      this.defaultFunction = defaultFunction;
      return this;
    }

    public String getFactor() {
      return factor;
    }

    public Row setFactor(String factor) {
      this.factor = factor;
      return this;
    }

    public String getDefaultFactor() {
      return defaultFactor;
    }

    public Row setDefaultFactor(String defaultFactor) {
      this.defaultFactor = defaultFactor;
      return this;
    }

    public String getOffset() {
      return offset;
    }

    public Row setOffset(String offset) {
      this.offset = offset;
      return this;
    }

    public String getDefaultOffset() {
      return defaultOffset;
    }

    public Row setDefaultOffset(String defaultOffset) {
      this.defaultOffset = defaultOffset;
      return this;
    }

    public String getStatus() {
      return status;
    }

    public Row setStatus(String status) {
      this.status = status;
      return this;
    }
  }

}
