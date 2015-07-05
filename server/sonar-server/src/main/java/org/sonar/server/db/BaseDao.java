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
package org.sonar.server.db;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.session.ResultContext;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Dto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.search.DbSynchronizationHandler;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.action.DeleteKey;
import org.sonar.server.search.action.DeleteNestedItem;
import org.sonar.server.search.action.InsertDto;
import org.sonar.server.search.action.RefreshIndex;
import org.sonar.server.search.action.UpsertDto;
import org.sonar.server.search.action.UpsertNestedItem;

import static com.google.common.collect.Maps.newHashMap;

/**
 * naming convention for DAO
 * =========================
 * <p/>
 * The DAO manages a Business Domain for a set of DTO. There is a Main DTO (i.e. RuleDto)
 * that has a few nested/child DTOs. The DAO supports nested DTOs for 1-to-1 and 1-to-many
 * relations. Many-to-many relations are handled by their own DAO classes (i.e. ActiveRuleDao)
 * <p/>
 * Main DTO
 * -------------------------
 * <p/>
 * * GET Methods
 * - returns a single DTO
 * - DTO is fully loaded (no field will return null)
 * - returns null (and not empty)
 * - examples:
 * - RuleDto = ruleDao.getNullableByKey(dto.getKey());
 * <p/>
 * * FIND Methods
 * - returns a List of DTO.
 * - Returns an empty list id no match
 * - method name is FULLY-NOMINATIVE
 * - examples:
 * - List<RuleDto> rules findByQualityProfile(QualityProfile qprofile)
 * - List<RuleDto> rules findByQualityProfile(QualityProfileKey qprofileKey)
 * - List<RuleDto> rules findByQualityProfileAndCreatedAfter(QualityProfileKey qprofileKey, Date date)
 * <p/>
 * * CRUD Methods
 * - insert(DTO)
 * - udpate(DTO)
 * - delete(DTO)
 * <p/>
 * Nested DTO
 * -------------------------
 * <p/>
 * Some DAO implementations also manage nested DTO. RuleTag for example is managed by the RuleDao class
 * Nested DTO are accessible following a similar convention for the Main DTO:
 * <p/>
 * * GET Methods
 * - returns a single DTO
 * - DTO is fully loaded (no field will return null)
 * - returns null (and not empty)
 * - prefixed with DTO type
 * - examples:
 * - RuleTagDto = ruleDao.getTagByKey(tagDto.getKey());
 * <p/>
 * * FIND Methods
 * - returns a List of DTO.
 * - Returns an empty list id no match
 * - method name is FULLY-NOMINATIVE
 * - prefixed with DTO type
 * - examples:
 * - List<RuleTagDto> tags findRuleTagByRuleKey(RuleKey key)
 * - List<RuleTagDto> tags findRuleTagByRepositoryAndLanguage(RepositoryKey key, String language)
 * <p/>
 * * CRUD Methods are slightly different because they REQUIRE the main DTO to be valid
 * - Nested dto methods MUST have the Main DTO or it's key as param
 * - add
 * - remove
 * - update
 * - examples:
 * - RuleTagDto tag add(RuleTagDto tag, RuleKey key)
 * - RuleParamDto param add(RuleParamDto param, RuleDto rule)
 *
 * @param <MAPPER> iBatis Mapper class
 * @param <DTO> Produced DTO class from this dao
 * @param <KEY> DTO Key class
 */
public abstract class BaseDao<MAPPER, DTO extends Dto<KEY>, KEY extends Serializable> implements DeprecatedDao<DTO, KEY>, Dao {

  private static final Logger LOGGER = Loggers.get(BaseDao.class);

  protected IndexDefinition indexDefinition;
  private Class<MAPPER> mapperClass;
  private System2 system2;

  protected boolean hasIndex() {
    return indexDefinition != null;
  }

  protected BaseDao(@Nullable IndexDefinition indexDefinition, Class<MAPPER> mapperClass, System2 system2) {
    this.mapperClass = mapperClass;
    this.indexDefinition = indexDefinition;
    this.system2 = system2;
  }

  public String getIndexType() {
    return indexDefinition != null ? this.indexDefinition.getIndexType() : null;
  }

  protected MAPPER mapper(DbSession session) {
    return session.getMapper(mapperClass);
  }

  @Override
  @CheckForNull
  public DTO getNullableByKey(DbSession session, KEY key) {
    return doGetNullableByKey(session, key);
  }

  @Override
  public DTO getByKey(DbSession session, KEY key) {
    DTO value = doGetNullableByKey(session, key);
    if (value == null) {
      throw new NotFoundException(String.format("Key '%s' not found", key));
    }
    return value;
  }

  @Override
  public DTO update(DbSession session, DTO item) {
    Date now = new Date(system2.now());
    update(session, item, now);
    return item;
  }

  @Override
  public DTO update(DbSession session, DTO item, DTO... others) {
    update(session, Lists.asList(item, others));
    return item;
  }

  @Override
  public Collection<DTO> update(DbSession session, Collection<DTO> items) {
    Date now = new Date(system2.now());
    for (DTO item : items) {
      update(session, item, now);
    }
    return items;
  }

  private void update(DbSession session, DTO item, Date now) {
    try {
      item.setUpdatedAt(now);
      doUpdate(session, item);
      if (hasIndex()) {
        session.enqueue(new UpsertDto<DTO>(getIndexType(), item));
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to update item in db: " + item, e);
    }
  }

  @Override
  public DTO insert(DbSession session, DTO item) {
    insert(session, item, new Date(system2.now()));
    return item;
  }

  @Override
  public Collection<DTO> insert(DbSession session, Collection<DTO> items) {
    Date now = new Date(system2.now());
    for (DTO item : items) {
      insert(session, item, now);
    }
    return items;
  }

  @Override
  public DTO insert(DbSession session, DTO item, DTO... others) {
    insert(session, Lists.<DTO>asList(item, others));
    return item;
  }

  private void insert(DbSession session, DTO item, Date now) {
    if (item.getCreatedAt() == null) {
      item.setCreatedAt(now);
    }
    item.setUpdatedAt(now);
    try {
      doInsert(session, item);
      if (hasIndex()) {
        session.enqueue(new UpsertDto<>(getIndexType(), item));
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to insert item in db: " + item, e);
    }
  }

  @Override
  public void delete(DbSession session, DTO item) {
    deleteByKey(session, item.getKey());
  }

  @Override
  public void delete(DbSession session, DTO item, DTO... others) {
    delete(session, Lists.<DTO>asList(item, others));
  }

  @Override
  public void delete(DbSession session, Collection<DTO> items) {
    for (DTO item : items) {
      delete(session, item);
    }
  }

  @Override
  public void deleteByKey(DbSession session, KEY key) {
    Preconditions.checkNotNull(key, "Missing key");
    try {
      doDeleteByKey(session, key);
      if (hasIndex()) {
        session.enqueue(new DeleteKey<>(getIndexType(), key));
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to delete item from db: " + key, e);
    }
  }

  protected final void enqueueUpdate(Object nestedItem, KEY key, DbSession session) {
    if (hasIndex()) {
      session.enqueue(new UpsertNestedItem<>(
        this.getIndexType(), key, nestedItem));
    }
  }

  public void enqueueDelete(Object nestedItem, KEY key, DbSession session) {
    if (hasIndex()) {
      session.enqueue(new DeleteNestedItem<>(
        this.getIndexType(), key, nestedItem));
      session.commit();
    }
  }

  public void enqueueInsert(Object nestedItem, KEY key, DbSession session) {
    if (hasIndex()) {
      this.enqueueUpdate(nestedItem, key, session);
    }
  }

  // Synchronization methods

  protected DbSynchronizationHandler getSynchronizationResultHandler(final DbSession session, Map<String, String> params) {
    return new DbSynchronizationHandler(session, params) {
      private int count = 0;

      @Override
      public void handleResult(ResultContext resultContext) {
        DTO dto = (DTO) resultContext.getResultObject();
        // session.enqueue(new UpsertDto<DTO>(getIndexType(), dto, false));
        getSession().enqueue(new InsertDto<>(getIndexType(), dto, false));
        count++;
        if (count % 100000 == 0) {
          LOGGER.info("Synchronized {} {}", count, getIndexType());
        }
      }

      @Override
      public void enqueueCollected() {
        // Do nothing in this case
      }
    };
  }

  protected Map<String, Object> getSynchronizationParams(@Nullable Date date, Map<String, String> params) {
    Map<String, Object> finalParams = newHashMap();
    if (date != null) {
      finalParams.put("date", new Timestamp(date.getTime()));
    }
    return finalParams;
  }

  @Override
  public void synchronizeAfter(final DbSession session) {
    this.synchronizeAfter(session, null, Collections.<String, String>emptyMap());
  }

  @Override
  public void synchronizeAfter(final DbSession session, @Nullable Date date) {
    this.synchronizeAfter(session, date, Collections.<String, String>emptyMap());
  }

  @Override
  public void synchronizeAfter(final DbSession session, @Nullable Date date, Map<String, String> params) {
    DbSynchronizationHandler handler = getSynchronizationResultHandler(session, params);
    session.select(getSynchronizeStatementFQN(), getSynchronizationParams(date, params), handler);
    handler.enqueueCollected();
    session.enqueue(new RefreshIndex(this.getIndexType()));
    session.commit();
  }

  private String getSynchronizeStatementFQN() {
    return mapperClass.getName() + "." + this.getSynchronizationStatementName();
  }

  @CheckForNull
  protected abstract DTO doGetNullableByKey(DbSession session, KEY key);

  protected String getSynchronizationStatementName() {
    return "selectAfterDate";
  }

  protected DTO doInsert(DbSession session, DTO item) {
    throw notImplemented(this);
  }

  protected DTO doUpdate(DbSession session, DTO item) {
    throw notImplemented(this);
  }

  protected void doDeleteByKey(DbSession session, KEY key) {
    throw notImplemented(this);
  }

  private static RuntimeException notImplemented(BaseDao baseDao) {
    throw new IllegalStateException("Not implemented yet for class [" + baseDao.getClass().getSimpleName() + "]");
  }
}
