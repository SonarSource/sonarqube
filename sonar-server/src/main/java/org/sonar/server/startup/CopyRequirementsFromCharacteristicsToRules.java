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
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.rules.Rule;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.Database;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.technicaldebt.db.RequirementDao;
import org.sonar.core.technicaldebt.db.RequirementDto;
import org.sonar.server.db.migrations.MassUpdater;
import org.sonar.server.db.migrations.SqlUtil;
import org.sonar.server.rule.RegisterRules;
import org.sonar.server.rule.RuleRegistry;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.sql.*;
import java.util.Collection;
import java.util.List;

/**
 * This script copy every requirements from characteristics table (every row where rule_id is not null) to the rules table.
 *
 * This script need to be executed after rules registration because default debt columns (characteristics, function, coefficient and offset) has to be populated
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

  private final RuleRegistry ruleRegistry;

  /**
   * @param registerRules used only to be started after init of rules
   */
  public CopyRequirementsFromCharacteristicsToRules(Database database, RequirementDao requirementDao, ServerUpgradeStatus status, RuleRegistry ruleRegistry,
                                                    RegisterRules registerRules) {
    this(database, requirementDao, ruleRegistry, status, System2.INSTANCE);
  }

  @VisibleForTesting
  CopyRequirementsFromCharacteristicsToRules(Database database, RequirementDao requirementDao, RuleRegistry ruleRegistry, ServerUpgradeStatus status, System2 system2) {
    this.db = database;
    this.system2 = system2;
    this.status = status;
    this.requirementDao = requirementDao;
    this.ruleRegistry = ruleRegistry;
  }

  public void start() {
    if (mustDoExecute()) {
      doExecute();
    }
  }

  private boolean mustDoExecute() {
    return status.isUpgraded() && status.getInitialDbVersion() <= 520;
  }

  private void doExecute() {
    LOGGER.info("Copying requirement from characteristics to rules");
    copyRequirementsFromCharacteristicsToRules();

    LOGGER.info("Deleting requirements data");
    removeRequirementsDataFromCharacteristics();

    LOGGER.info("Reindex rules in E/S");
    ruleRegistry.reindex();
  }

  private void copyRequirementsFromCharacteristicsToRules() {
    List<RequirementDto> requirementDtos = requirementDao.selectRequirements();
    final Multimap<Integer, RequirementDto> requirementsByRuleId = ArrayListMultimap.create();
    for (RequirementDto requirementDto : requirementDtos) {
      requirementsByRuleId.put(requirementDto.getRuleId(), requirementDto);
    }

    new MassUpdater(db).execute(
      new RuleInputLoader(),
      new RuleInputConvertor(requirementsByRuleId, system2)
    );
  }

  private static class RuleInputLoader implements MassUpdater.InputLoader<RuleRow>{
    @Override
    public String selectSql() {
      return "SELECT r.id,r.characteristic_id,r.remediation_function,r.remediation_coeff,r.remediation_offset," +
        "r.default_characteristic_id,r.default_remediation_function,r.default_remediation_coeff,r.default_remediation_offset,r.status " +
        "FROM rules r";
    }

    @Override
    public RuleRow load(ResultSet rs) throws SQLException {
      RuleRow ruleRow = new RuleRow();
      ruleRow.setId(SqlUtil.getInt(rs, 1));
      ruleRow.setCharacteristicId(SqlUtil.getInt(rs, 2));
      ruleRow.setFunction(rs.getString(3));
      ruleRow.setFactor(rs.getString(4));
      ruleRow.setOffset(rs.getString(5));
      ruleRow.setDefaultCharacteristicId(SqlUtil.getInt(rs, 6));
      ruleRow.setDefaultFunction(rs.getString(7));
      ruleRow.setDefaultFactor(rs.getString(8));
      ruleRow.setDefaultOffset(rs.getString(9));
      ruleRow.setStatus(rs.getString(10));
      return ruleRow;
    }
  }

  private static class RuleInputConvertor implements MassUpdater.InputConverter<RuleRow>{

    private final Multimap<Integer, RequirementDto> requirementsByRuleId;
    private final System2 system2;

    private RuleInputConvertor(Multimap<Integer, RequirementDto> requirementsByRuleId, System2 system2) {
      this.requirementsByRuleId = requirementsByRuleId;
      this.system2 = system2;
    }

    @Override
    public String updateSql() {
      return "UPDATE rules SET characteristic_id=?,remediation_function=?,remediation_coeff=?,remediation_offset=?,updated_at=? WHERE id=?";
    }

    @Override
    public boolean convert(RuleRow ruleRow, PreparedStatement updateStatement) throws SQLException {
      Collection<RequirementDto> requirementsForRule = requirementsByRuleId.get(ruleRow.id);
      if (!requirementsForRule.isEmpty()) {
        return convert(ruleRow, updateStatement, requirementsForRule);
      }
      // Nothing to do when no requirements for current rule
      return false;
    }

    private boolean convert(RuleRow ruleRow, PreparedStatement updateStatement, Collection<RequirementDto> requirementsForRule) throws SQLException {
      RequirementDto enabledRequirement = Iterables.find(requirementsForRule, new Predicate<RequirementDto>() {
        @Override
        public boolean apply(RequirementDto input) {
          return input.isEnabled();
        }
      }, null);

      if (enabledRequirement == null && !Rule.STATUS_REMOVED.equals(ruleRow.getStatus())) {
        // If no enabled requirement is found, it means that the requirement has been disabled for this rule
        updateStatement.setInt(1, RuleDto.DISABLED_CHARACTERISTIC_ID);
        updateStatement.setNull(2, Types.VARCHAR);
        updateStatement.setNull(3, Types.VARCHAR);
        updateStatement.setNull(4, Types.VARCHAR);
        updateStatement.setTimestamp(5, new Timestamp(system2.now()));
        updateStatement.setInt(6, ruleRow.getId());
        return true;

      } else if (enabledRequirement != null) {
        // If one requirement is enable, it means either that this requirement has been set from SQALE, or that it come from a XML model definition

        ruleRow.setCharacteristicId(enabledRequirement.getParentId());
        ruleRow.setFunction(enabledRequirement.getFunction().toUpperCase());
        ruleRow.setFactor(convertDuration(enabledRequirement.getFactorValue(), enabledRequirement.getFactorUnit()));
        ruleRow.setOffset(convertDuration(enabledRequirement.getOffsetValue(), enabledRequirement.getOffsetUnit()));

        if (!isDebtDefaultValuesSameAsOverriddenValues(ruleRow)) {
          // Default values on debt are not the same that ones set by SQALE, update the rule
          updateStatement.setInt(1, ruleRow.getCharacteristicId());
          updateStatement.setString(2, ruleRow.getFunction());
          updateStatement.setString(3, ruleRow.getFactor());
          updateStatement.setString(4, ruleRow.getOffset());
          updateStatement.setTimestamp(5, new Timestamp(system2.now()));
          updateStatement.setInt(6, ruleRow.getId());
          return true;
        }
        // When default values on debt are the same that ones set by SQALE, nothing to do
      }
      return false;
    }
  }

  @CheckForNull
  @VisibleForTesting
  static String convertDuration(@Nullable Double oldValue, @Nullable String oldUnit) {
    if (oldValue != null && oldValue > 0) {
      String unit = oldUnit != null ? oldUnit : Duration.DAY;
      // min is replaced by mn
      unit = "mn".equals(unit) ? Duration.MINUTE : unit;
      // As value is stored in double, we have to round it in order to have an integer (for instance, if it was 1.6, we'll use 2)
      return Integer.toString((int) Math.round(oldValue)) + unit;
    }
    return null;
  }

  @VisibleForTesting
  static boolean isDebtDefaultValuesSameAsOverriddenValues(RuleRow ruleRow) {
    return new EqualsBuilder()
      .append(ruleRow.getDefaultCharacteristicId(), ruleRow.getCharacteristicId())
      .append(ruleRow.getDefaultFunction(), ruleRow.getFunction())
      .append(ruleRow.getDefaultFactor(), ruleRow.getFactor())
      .append(ruleRow.getDefaultOffset(), ruleRow.getOffset())
      .isEquals();
  }

  private void removeRequirementsDataFromCharacteristics() {
    Connection connection = null;
    Statement stmt = null;
    try {
      connection = db.getDataSource().getConnection();
      stmt = connection.createStatement();
      stmt.executeUpdate("DELETE FROM characteristics WHERE rule_id IS NOT NULL");
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to remove requirements data from characteristics", e);
    } finally {
      DbUtils.closeQuietly(stmt);
      DbUtils.closeQuietly(connection);
    }
  }

  @VisibleForTesting
  static class RuleRow {
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

    Integer getId() {
      return id;
    }

    RuleRow setId(Integer id) {
      this.id = id;
      return this;
    }

    @CheckForNull
    Integer getCharacteristicId() {
      return characteristicId;
    }

    RuleRow setCharacteristicId(@Nullable Integer characteristicId) {
      this.characteristicId = characteristicId;
      return this;
    }

    @CheckForNull
    Integer getDefaultCharacteristicId() {
      return defaultCharacteristicId;
    }

    RuleRow setDefaultCharacteristicId(@Nullable Integer defaultCharacteristicId) {
      this.defaultCharacteristicId = defaultCharacteristicId;
      return this;
    }

    @CheckForNull
    String getFunction() {
      return function;
    }

    RuleRow setFunction(@Nullable String function) {
      this.function = function;
      return this;
    }

    @CheckForNull
    String getDefaultFunction() {
      return defaultFunction;
    }

    RuleRow setDefaultFunction(@Nullable String defaultFunction) {
      this.defaultFunction = defaultFunction;
      return this;
    }

    @CheckForNull
    String getFactor() {
      return factor;
    }

    RuleRow setFactor(@Nullable String factor) {
      this.factor = factor;
      return this;
    }

    @CheckForNull
    String getDefaultFactor() {
      return defaultFactor;
    }

    RuleRow setDefaultFactor(@Nullable String defaultFactor) {
      this.defaultFactor = defaultFactor;
      return this;
    }

    @CheckForNull
    String getOffset() {
      return offset;
    }

    RuleRow setOffset(@Nullable String offset) {
      this.offset = offset;
      return this;
    }

    @CheckForNull
    String getDefaultOffset() {
      return defaultOffset;
    }

    RuleRow setDefaultOffset(@Nullable String defaultOffset) {
      this.defaultOffset = defaultOffset;
      return this;
    }

    String getStatus() {
      return status;
    }

    RuleRow setStatus(String status) {
      this.status = status;
      return this;
    }
  }

}
