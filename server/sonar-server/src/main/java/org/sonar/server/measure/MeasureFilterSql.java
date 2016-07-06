/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.measure;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.WildcardPosition;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;

import static org.sonar.db.component.ComponentDto.UUID_PATH_SEPARATOR;

class MeasureFilterSql {

  private final Database database;
  private final MeasureFilter filter;
  private final MeasureFilterContext context;
  private final String sql;
  private final List<Long> dateParameters = Lists.newArrayList();

  MeasureFilterSql(Database database, MeasureFilter filter, MeasureFilterContext context) {
    this.database = database;
    this.filter = filter;
    this.context = context;
    this.sql = generateSql();
  }

  private static void appendInStatement(List<String> values, StringBuilder to) {
    to.append(" (");
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        to.append(",");
      }
      to.append("'");
      to.append(StringEscapeUtils.escapeSql(values.get(i)));
      to.append("'");
    }
    to.append(") ");
  }

  private static Ordering newObjectOrdering(boolean ascending) {
    if (ascending) {
      return Ordering.from(new AscendingComparator());
    }
    return Ordering.from(new DescendingComparator());
  }

  List<MeasureFilterRow> execute(Connection connection) throws SQLException {
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = null;
    try {
      for (int index = 0; index < dateParameters.size(); index++) {
        statement.setLong(index + 1, dateParameters.get(index));
      }
      rs = statement.executeQuery();
      return process(rs);

    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(statement);
    }
  }

  String sql() {
    return sql;
  }

  private String generateSql() {
    StringBuilder sb = new StringBuilder(1000);
    sb.append("select c.uuid, c.project_uuid, ");
    sb.append(filter.sort().column());
    sb.append(" from projects c");
    sb.append(" inner join snapshots s on s.component_uuid=c.project_uuid ");
    if (context.getBaseComponent() != null) {
      sb.append(" inner join projects base on base.project_uuid = c.project_uuid ");
    }

    for (int index = 0; index < filter.getMeasureConditions().size(); index++) {
      MeasureFilterCondition condition = filter.getMeasureConditions().get(index);
      sb.append(" inner join project_measures pmcond").append(index);
      sb.append(" on pmcond").append(index).append(".analysis_uuid = s.uuid and ");
      sb.append(" pmcond").append(index).append(".component_uuid = c.uuid and ");
      condition.appendSqlCondition(sb, index);
    }

    if (filter.isOnFavourites()) {
      sb.append(" inner join properties props on props.resource_id=c.id ");
    }

    if (filter.sort().isOnMeasure()) {
      sb.append(" left outer join project_measures pmsort ON s.uuid = pmsort.analysis_uuid and pmsort.component_uuid = c.uuid and pmsort.metric_id=");
      sb.append(filter.sort().metric().getId());
      sb.append(" and pmsort.person_id is null ");
    }

    sb.append(" where ");
    sb.append(" s.islast=").append(database.getDialect().getTrueSqlValue());
    appendComponentConditions(sb);

    for (int index = 0; index < filter.getMeasureConditions().size(); index++) {
      MeasureFilterCondition condition = filter.getMeasureConditions().get(index);
      sb.append(" and ");
      condition.appendSqlCondition(sb, index);
    }

    return sb.toString();
  }

  private void appendComponentConditions(StringBuilder sb) {
    sb.append(" and c.enabled=").append(database.getDialect().getTrueSqlValue());
    ComponentDto base = context.getBaseComponent();
    if (base == null) {
      sb.append(" and c.copy_component_uuid is null ");
    } else {
      sb.append(" and base.uuid = '").append(base.uuid()).append("' ");
      if (filter.isOnBaseResourceChildren()) {
        String path = base.getUuidPath() + base.uuid() + UUID_PATH_SEPARATOR;
        sb.append(" and c.uuid_path = '").append(path).append("' ");
      } else {
        String like =  DatabaseUtils.buildLikeValue(base.getUuidPath() + base.uuid() + UUID_PATH_SEPARATOR, WildcardPosition.AFTER);
        sb.append(" and c.uuid_path like '").append(like).append("' escape '/' ");
      }
    }
    if (!filter.getResourceQualifiers().isEmpty()) {
      sb.append(" and c.qualifier in ");
      appendInStatement(filter.getResourceQualifiers(), sb);
    }
    if (!filter.getResourceScopes().isEmpty()) {
      sb.append(" and c.scope in ");
      appendInStatement(filter.getResourceScopes(), sb);
    }
    appendDateConditions(sb);
    appendFavouritesCondition(sb);
    appendResourceNameCondition(sb);
    appendResourceKeyCondition(sb);
  }

  private void appendDateConditions(StringBuilder sb) {
    Date fromDate = filter.getFromDate();
    if (fromDate != null) {
      sb.append(" and s.created_at >= ? ");
      dateParameters.add(fromDate.getTime());
    }
    Date toDate = filter.getToDate();
    if (toDate != null) {
      sb.append(" and s.created_at <= ? ");
      dateParameters.add(toDate.getTime());
    }
  }

  private void appendFavouritesCondition(StringBuilder sb) {
    if (filter.isOnFavourites()) {
      sb.append(" and props.prop_key='favourite' and props.resource_id is not null and props.user_id=");
      sb.append(context.getUserId());
      sb.append(" ");
    }
  }

  private void appendResourceKeyCondition(StringBuilder sb) {
    if (StringUtils.isNotBlank(filter.getResourceKey())) {
      sb.append(" and UPPER(c.kee) like '%");
      sb.append(escapePercentAndUnderscrore(StringEscapeUtils.escapeSql(StringUtils.upperCase(filter.getResourceKey()))));
      sb.append("%'");
      appendEscapeForSomeDb(sb);
    }
  }

  private void appendResourceNameCondition(StringBuilder sb) {
    if (StringUtils.isNotBlank(filter.getResourceName())) {
      sb.append(" and c.uuid in (select rindex.component_uuid from resource_index rindex WHERE rindex.kee LIKE '");
      sb.append(escapePercentAndUnderscrore(StringEscapeUtils.escapeSql(StringUtils.lowerCase(filter.getResourceName()))));
      sb.append("%'");
      appendEscapeForSomeDb(sb);
      if (!filter.getResourceQualifiers().isEmpty()) {
        sb.append(" AND rindex.qualifier IN ");
        appendInStatement(filter.getResourceQualifiers(), sb);
      }
      sb.append(") ");
    }
  }

  List<MeasureFilterRow> process(ResultSet rs) throws SQLException {
    List<MeasureFilterRow> rows = Lists.newArrayList();
    RowProcessor rowProcessor;
    if (filter.sort().isOnNumericMeasure()) {
      rowProcessor = new NumericSortRowProcessor();
    } else if (filter.sort().isOnDate()) {
      rowProcessor = new DateSortRowProcessor();
    } else if (filter.sort().isOnTime()) {
      rowProcessor = new LongSortRowProcessor();
    } else if (filter.sort().isOnAlert()) {
      rowProcessor = new AlertSortRowProcessor();
    } else {
      rowProcessor = new TextSortRowProcessor();
    }

    while (rs.next()) {
      rows.add(rowProcessor.fetch(rs));
    }

    return rowProcessor.sort(rows, filter.sort().isAsc());
  }

  /**
   * Replace escape percent and underscore by adding a slash just before
   */
  private static String escapePercentAndUnderscrore(String value) {
    return value.replaceAll("%", "\\\\%").replaceAll("_", "\\\\_");
  }

  private void appendEscapeForSomeDb(StringBuilder sb) {
    if (database.getDialect().getId().equals(Oracle.ID) || database.getDialect().getId().equals(MsSql.ID)) {
      sb.append(" ESCAPE '\\'");
    }
  }

  abstract static class RowProcessor {
    abstract Function sortFieldFunction();

    abstract Ordering sortFieldOrdering(boolean ascending);

    abstract MeasureFilterRow fetch(ResultSet rs) throws SQLException;

    final List<MeasureFilterRow> sort(List<MeasureFilterRow> rows, boolean ascending) {
      Ordering<MeasureFilterRow> ordering = sortFieldOrdering(ascending).onResultOf(sortFieldFunction());
      return ordering.immutableSortedCopy(rows);
    }
  }

  static class TextSortRowProcessor extends RowProcessor {
    @Override
    MeasureFilterRow fetch(ResultSet rs) throws SQLException {
      MeasureFilterRow row = new MeasureFilterRow(rs.getString(1), rs.getString(2));
      row.setSortText(rs.getString(3));
      return row;
    }

    @Override
    Function sortFieldFunction() {
      return new Function<MeasureFilterRow, String>() {
        @Override
        public String apply(MeasureFilterRow row) {
          return row.getSortText();
        }
      };
    }

    @Override
    Ordering sortFieldOrdering(boolean ascending) {
      Ordering<String> ordering = Ordering.from(String.CASE_INSENSITIVE_ORDER);
      if (!ascending) {
        ordering = ordering.reverse();
      }
      return ordering;
    }
  }

  static class AlertSortRowProcessor extends TextSortRowProcessor {
    @Override
    Function sortFieldFunction() {
      return new MeasureFilterRowToAlertIndexFunction();
    }

    @Override
    Ordering sortFieldOrdering(boolean ascending) {
      Ordering<Integer> ordering = Ordering.<Integer>natural().nullsLast();
      if (!ascending) {
        ordering = ordering.reverse();
      }
      return ordering;
    }

  }
  static class NumericSortRowProcessor extends RowProcessor {

    @Override
    MeasureFilterRow fetch(ResultSet rs) throws SQLException {
      MeasureFilterRow row = new MeasureFilterRow(rs.getString(1), rs.getString(2));
      double value = rs.getDouble(3);
      if (!rs.wasNull()) {
        row.setSortDouble(value);
      }
      return row;
    }
    @Override
    Function sortFieldFunction() {
      return new MeasureFilterRowToSortDoubleFunction();
    }

    @Override
    Ordering sortFieldOrdering(boolean ascending) {
      return ascending ? Ordering.natural().nullsLast() : Ordering.natural().reverse().nullsLast();
    }

    private static class MeasureFilterRowToSortDoubleFunction implements Function<MeasureFilterRow, Double> {

      @Override
      public Double apply(MeasureFilterRow row) {
        return row.getSortDouble();
      }
    }
  }
  static class DateSortRowProcessor extends RowProcessor {

    @Override
    MeasureFilterRow fetch(ResultSet rs) throws SQLException {
      MeasureFilterRow row = new MeasureFilterRow(rs.getString(1), rs.getString(2));
      row.setSortDate(rs.getTimestamp(3).getTime());
      return row;
    }
    @Override
    Function sortFieldFunction() {
      return new MeasureFilterRowToSortDateFunction();
    }

    @Override
    Ordering sortFieldOrdering(boolean ascending) {
      return newObjectOrdering(ascending);
    }

  }
  static class LongSortRowProcessor extends RowProcessor {

    @Override
    MeasureFilterRow fetch(ResultSet rs) throws SQLException {
      MeasureFilterRow row = new MeasureFilterRow(rs.getString(1), rs.getString(2));
      row.setSortDate(rs.getLong(3));
      return row;
    }

    @Override
    Function sortFieldFunction() {
      return new MeasureFilterRowToSortDateFunction();
    }

    @Override
    Ordering sortFieldOrdering(boolean ascending) {
      return newObjectOrdering(ascending);
    }

  }

  private static class MeasureFilterRowToAlertIndexFunction implements Function<MeasureFilterRow, Integer> {
    @Override
    public Integer apply(MeasureFilterRow row) {
      return ImmutableList.of("OK", "WARN", "ERROR").indexOf(row.getSortText());
    }
  }

  private static class MeasureFilterRowToSortDateFunction implements Function<MeasureFilterRow, Long> {
    @Override
    public Long apply(MeasureFilterRow row) {
      return row.getSortDate();
    }
  }

  private static class AscendingComparator implements Comparator<Comparable> {
    @Override
    public int compare(@Nullable Comparable left, @Nullable Comparable right) {
      if (left == null) {
        return 1;
      }
      if (right == null) {
        return -1;
      }

      return left.compareTo(right);
    }
  }

  private static class DescendingComparator implements Comparator<Comparable> {
    @Override
    public int compare(@Nullable Comparable left, @Nullable Comparable right) {
      if (left == null) {
        return 1;
      }
      if (right == null) {
        return -1;
      }

      return right.compareTo(left);
    }
  }
}
