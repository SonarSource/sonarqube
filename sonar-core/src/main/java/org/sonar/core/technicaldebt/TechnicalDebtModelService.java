/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.core.technicaldebt;

import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerExtension;
import org.sonar.api.rules.Rule;
import org.sonar.api.technicaldebt.Characteristic;
import org.sonar.api.technicaldebt.Requirement;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;

import javax.annotation.Nullable;

public class TechnicalDebtModelService implements ServerExtension {

  private final MyBatis mybatis;
  private final CharacteristicDao dao;

  public TechnicalDebtModelService(MyBatis mybatis, CharacteristicDao dao) {
    this.mybatis = mybatis;
    this.dao = dao;
  }

  public void create(Characteristic characteristic, @Nullable Integer parentId, SqlSession session) {
    CharacteristicDto characteristicDto = CharacteristicDto.toDto(characteristic, parentId);
    dao.insert(characteristicDto, session);
    characteristic.setId(characteristicDto.getId());
  }

  public void create(Characteristic characteristic, @Nullable Integer parentId) {
    SqlSession session = mybatis.openSession();
    try {
      create(characteristic, parentId, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void create(Requirement requirement, Integer characteristicId, TechnicalDebtRuleCache ruleCache, SqlSession session) {
    Rule rule = ruleCache.getByRuleKey(requirement.ruleKey());
    CharacteristicDto requirementDto = CharacteristicDto.toDto(requirement, characteristicId, rule.getId());
    dao.insert(requirementDto, session);
    requirement.setId(requirementDto.getId());
  }

  public void create(Requirement requirement, Integer characteristicId, TechnicalDebtRuleCache ruleCache) {
    SqlSession session = mybatis.openSession();
    try {
      create(requirement, characteristicId, ruleCache, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void disable(Requirement requirement, SqlSession session) {
    dao.disable(requirement.id(), session);
  }

  public void disable(Requirement requirement) {
    SqlSession session = mybatis.openSession();
    try {
      disable(requirement, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

}
