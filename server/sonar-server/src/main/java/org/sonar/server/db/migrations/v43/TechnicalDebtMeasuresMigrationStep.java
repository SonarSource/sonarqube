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

package org.sonar.server.db.migrations.v43;

import java.sql.SQLException;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.sonar.core.persistence.Database;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.server.db.migrations.BaseDataChange;
import org.sonar.server.db.migrations.MassUpdate;
import org.sonar.server.db.migrations.Select;
import org.sonar.server.db.migrations.SqlStatement;

/**
 * Used in the Active Record Migration 515
 *
 * @since 4.3
 */
public class TechnicalDebtMeasuresMigrationStep extends BaseDataChange {

  private final WorkDurationConvertor workDurationConvertor;

  public TechnicalDebtMeasuresMigrationStep(Database database, PropertiesDao propertiesDao) {
    super(database);
    this.workDurationConvertor = new WorkDurationConvertor(propertiesDao);
  }

  @Override
  public void execute(Context context) throws SQLException {
    workDurationConvertor.init();

    List<Long> metricIds = context.prepareSelect("select id from metrics " +
      "where name='sqale_index' or name='new_technical_debt' " +
      "or name='sqale_effort_to_grade_a' or name='sqale_effort_to_grade_b' or name='sqale_effort_to_grade_c' " +
      "or name='sqale_effort_to_grade_d' or name='blocker_remediation_cost' or name='critical_remediation_cost' " +
      "or name='major_remediation_cost' or name='minor_remediation_cost' or name='info_remediation_cost'").list(Select.LONG_READER);

    if (!metricIds.isEmpty()) {
      MassUpdate massUpdate = context.prepareMassUpdate();
      massUpdate.rowPluralName("measures");

      SqlStatement select = massUpdate.select("SELECT pm.id, pm.value " +
        ", pm.variation_value_1 , pm.variation_value_2, pm.variation_value_3 " +
        ", pm.variation_value_4 , pm.variation_value_5 " +
        " FROM project_measures pm " +
        " WHERE pm.metric_id IN (" + StringUtils.repeat("?", ",", metricIds.size()) + ")");
      for (int i = 0; i < metricIds.size(); i++) {
        select.setLong(i + 1, metricIds.get(i));
      }
      massUpdate.update("UPDATE project_measures SET value=?," +
        "variation_value_1=?,variation_value_2=?,variation_value_3=?,variation_value_4=?,variation_value_5=? WHERE id=?");
      massUpdate.execute(new Converter());
    }
  }

  private class Converter implements MassUpdate.Handler {
    @Override
    public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
      Long id = row.getNullableLong(1);
      Double value = row.getNullableDouble(2);
      Double var1 = row.getNullableDouble(3);
      Double var2 = row.getNullableDouble(4);
      Double var3 = row.getNullableDouble(5);
      Double var4 = row.getNullableDouble(6);
      Double var5 = row.getNullableDouble(7);

      update.setLong(1, convertDebtForDays(value));
      update.setLong(2, convertDebtForDays(var1));
      update.setLong(3, convertDebtForDays(var2));
      update.setLong(4, convertDebtForDays(var3));
      update.setLong(5, convertDebtForDays(var4));
      update.setLong(6, convertDebtForDays(var5));
      update.setLong(7, id);
      return true;
    }

    @CheckForNull
    private Long convertDebtForDays(@Nullable Double data) {
      if (data != null) {
        return workDurationConvertor.createFromDays(data);
      }
      return null;
    }
  }
}
