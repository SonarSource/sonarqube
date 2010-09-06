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
package org.sonar.jpa.dao;

public class DaoFacade {

  private final RulesDao rulesDao;
  private final MeasuresDao measuresDao;
  private final AsyncMeasuresDao asyncMeasureDao;
  private final ProfilesDao profilesDao;

  public DaoFacade(ProfilesDao profilesDao, RulesDao rulesDao, MeasuresDao measuresDao, AsyncMeasuresDao asyncMeasureDao) {
    super();
    this.rulesDao = rulesDao;
    this.measuresDao = measuresDao;
    this.asyncMeasureDao = asyncMeasureDao;
    this.profilesDao = profilesDao;
  }

  public RulesDao getRulesDao() {
    return rulesDao;
  }

  public ProfilesDao getProfilesDao() {
    return profilesDao;
  }

  public MeasuresDao getMeasuresDao() {
    return measuresDao;
  }

  public AsyncMeasuresDao getAsyncMeasureDao() {
    return asyncMeasureDao;
  }

}
