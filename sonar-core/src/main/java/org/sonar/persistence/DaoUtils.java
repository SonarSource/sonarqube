/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.persistence;

import java.util.Arrays;
import java.util.List;

import org.sonar.persistence.dashboard.ActiveDashboardDao;
import org.sonar.persistence.dashboard.DashboardDao;
import org.sonar.persistence.duplication.DuplicationDao;
import org.sonar.persistence.review.ReviewDao;
import org.sonar.persistence.rule.RuleDao;
import org.sonar.persistence.template.LoadedTemplateDao;

public final class DaoUtils {

  private DaoUtils() {
  }

  public static List<Class<?>> getDaoClasses() {
    return Arrays.<Class<?>> asList(RuleDao.class, DuplicationDao.class, ReviewDao.class, ActiveDashboardDao.class, DashboardDao.class,
        LoadedTemplateDao.class);
  }
}
