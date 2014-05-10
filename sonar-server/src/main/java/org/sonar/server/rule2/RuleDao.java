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
package org.sonar.server.rule2;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleMapper;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.db.BaseDao;
import org.sonar.server.search.EmbeddedIndexAction;
import org.sonar.server.search.IndexAction;

import javax.annotation.CheckForNull;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RuleDao extends BaseDao<RuleMapper, RuleDto, RuleKey> implements BatchComponent, ServerComponent {

  public RuleDao() {
    super(new RuleIndexDefinition(), RuleMapper.class);
  }

  @CheckForNull
  @Override
  public RuleDto doGetByKey(RuleKey key, DbSession session) {
    return mapper(session).selectByKey(key);
  }

  @Override
  protected RuleDto doInsert(RuleDto item, DbSession session) {
    mapper(session).insert(item);
    return item;
  }

  @Override
  protected RuleDto doUpdate(RuleDto item, DbSession session) {
    mapper(session).update(item);
    return item;
  }

  @Override
  protected void doDeleteByKey(RuleKey key, DbSession session) {
    throw new UnsupportedOperationException("Rules cannot be deleted");
  }

  @CheckForNull
  @Deprecated
  public RuleDto getById(int id, DbSession session) {
    return mapper(session).selectById(id);
  }

  @CheckForNull
  public RuleDto getParent(RuleDto rule, DbSession session){
    Preconditions.checkNotNull(rule.getParentId(), "Rule has no persisted parent!");
    return mapper(session).selectById(rule.getParentId());
  }

  @Override
  public Collection<RuleKey> keysOfRowsUpdatedAfter(long timestamp, DbSession session) {
    final List<RuleKey> keys = Lists.newArrayList();
    session.select("selectKeysOfRulesUpdatedSince", new Timestamp(timestamp), new ResultHandler() {
      @Override
      public void handleResult(ResultContext context) {
        Map<String, String> map = (Map) context.getResultObject();
        keys.add(RuleKey.of(map.get("repo"), map.get("rule")));
      }
    });
    return keys;
  }

  /** Finder methods for Rules */

  public List<RuleDto> findByNonManual(DbSession session){
    return mapper(session).selectNonManual();
  }

  public List<RuleDto> findAll(DbSession session) {
    return mapper(session).selectAll();
  }

  public List<RuleDto> findByEnabledAndNotManual(DbSession session) {
    return mapper(session).selectEnablesAndNonManual();
  }

  public List<RuleDto> findByName(String name, DbSession session) {
    //TODO change selectByName to return a list
    return ImmutableList.of(mapper(session).selectByName(name));
  }

  /** Nested DTO RuleParams */

  public void addRuleParam(RuleDto rule, RuleParamDto paramDto, DbSession session) {
    Preconditions.checkNotNull(rule.getId(), "Rule id must be set");
    paramDto.setRuleId(rule.getId());
    mapper(session).insertParameter(paramDto);
    session.enqueue(new EmbeddedIndexAction<RuleKey>(this.getIndexType(), IndexAction.Method.INSERT, paramDto, rule.getKey()));
  }

  public RuleParamDto updateRuleParam(RuleDto rule, RuleParamDto paramDto, DbSession session) {
    Preconditions.checkNotNull(rule.getId(), "Rule id must be set");
    Preconditions.checkNotNull(paramDto.getId(), "Param is not yet persisted must be set");
    paramDto.setRuleId(rule.getId());
    System.out.println("paramDto = " + paramDto);
    session.enqueue(new EmbeddedIndexAction<RuleKey>(this.getIndexType(), IndexAction.Method.UPDATE, paramDto, rule.getKey()));
     mapper(session).updateParameter(paramDto);
    return paramDto;
  }

  public void removeRuleParam(RuleDto rule, RuleParamDto param, DbSession session) {
    Preconditions.checkNotNull(param.getId(), "Param is not persisted");
    mapper(session).deleteParameter(param.getId());
    session.enqueue(new EmbeddedIndexAction<RuleKey>(this.getIndexType(), IndexAction.Method.DELETE, param, rule.getKey()));
  }

  /** Finder methods for RuleParams */

  public List<RuleParamDto> findAllRuleParams(DbSession session) {
    return mapper(session).selectAllParams();
  }

  public List<RuleParamDto> findRuleParamsByRuleKey(RuleKey key, DbSession session) {
    return mapper(session).selectParamsByRuleKey(key);
  }

  public List<RuleParamDto> findRuleParamsByRules(List<RuleDto> ruleDtos, DbSession session) {
    List<RuleParamDto> ruleParamDtos = new ArrayList<RuleParamDto>();
    for(RuleDto rule:ruleDtos){
      ruleParamDtos.addAll(findRuleParamsByRuleKey(rule.getKey(), session));
    }
    return ruleParamDtos;
  }

  public RuleParamDto getRuleParamByRuleAndParamKey(RuleDto rule, String key, DbSession session) {
    Preconditions.checkNotNull(rule.getId(), "Rule is not persisted");
    return mapper(session).selectParamByRuleAndKey(rule.getId(), key);
  }
}
