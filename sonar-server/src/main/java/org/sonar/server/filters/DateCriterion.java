/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.filters;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.lang.time.DateUtils;

import java.util.Calendar;
import java.util.Date;

public class DateCriterion {

  private String operator;
  private Date date;

  public DateCriterion(String operator, Date date) {
    this.operator = operator;
    this.date = date;
  }

  public DateCriterion() {
  }

  public String getOperator() {
    return operator;
  }

  public DateCriterion setOperator(String operator) {
    this.operator = operator;
    return this;
  }

  public Date getDate() {
    return date;
  }

  public DateCriterion setDate(Date date) {
    this.date = date;
    return this;
  }

  public DateCriterion setDate(int daysAgo) {
    this.date = DateUtils.addDays(new Date(), -daysAgo);
    this.date = DateUtils.truncate(this.date, Calendar.DATE);
    return this;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("operator", operator)
        .append("date", date)
        .toString();
  }
}
