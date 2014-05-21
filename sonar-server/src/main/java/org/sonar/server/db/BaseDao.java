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
import org.sonar.core.persistence.DaoComponent;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.Dto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.action.DtoIndexAction;
import org.sonar.server.search.action.EmbeddedIndexAction;
import org.sonar.server.search.action.IndexAction;
import org.sonar.server.search.action.KeyIndexAction;

import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

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
 * - RuleDto = ruleDao.getByKey(dto.getKey());
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
 * @param <M> iBatis Mapper class
 * @param <E> Produced DTO class from this dao
 * @param <K> DTO Key class
 */
public abstract class BaseDao<M, E extends Dto<K>, K extends Serializable> implements Dao<E, K>, DaoComponent {

  protected IndexDefinition indexDefinition;
  private Class<M> mapperClass;
  private System2 system2;

  private boolean hasIndex() {
    return indexDefinition != null;
  }

  protected BaseDao(@Nullable IndexDefinition indexDefinition, Class<M> mapperClass, System2 system2) {
    this.mapperClass = mapperClass;
    this.indexDefinition = indexDefinition;
    this.system2 = system2;
  }

  protected BaseDao(Class<M> mapperClass, System2 system2) {
    this(null, mapperClass, system2);
  }

  public String getIndexType() {
    return indexDefinition != null ? this.indexDefinition.getIndexType() : null;
  }

  protected abstract E doGetByKey(K key, DbSession session);

  protected abstract E doInsert(E item, DbSession session);

  protected abstract E doUpdate(E item, DbSession session);

  protected abstract void doDeleteByKey(K key, DbSession session);

  protected M mapper(DbSession session) {
    return session.getMapper(mapperClass);
  }

  public E getByKey(K key, DbSession session) {
    return doGetByKey(key, session);
  }

  public E getNonNullByKey(K key, DbSession session) {
    E value = doGetByKey(key, session);
    if (value == null) {
      throw new NotFoundException(String.format("Key '%s' not found", key));
    }
    return value;
  }

  @Override
  public E update(E item, DbSession session) {
    item.setUpdatedAt(new Date(system2.now()));
    this.doUpdate(item, session);
    if (hasIndex()) {
      session.enqueue(new DtoIndexAction<E>(this.getIndexType(), IndexAction.Method.UPDATE, item));
    }
    return item;
  }

  @Override
  public List<E> update(List<E> items, DbSession session) {
    //TODO check for bulk inserts
    List<E> results = new ArrayList<E>();
    for (E item : items) {
      results.add(this.update(item, session));
    }
    return items;
  }

  @Override
  public E insert(E item, DbSession session) {
    item.setCreatedAt(new Date(system2.now()));
    item.setUpdatedAt(item.getCreatedAt());
    this.doInsert(item, session);
    if (hasIndex()) {
      session.enqueue(new DtoIndexAction<E>(this.getIndexType(), IndexAction.Method.INSERT, item));
    }
    return item;
  }

  @Override
  public List<E> insert(List<E> items, DbSession session) {
    //TODO check for bulk inserts
    List<E> results = new ArrayList<E>();
    for (E item : items) {
      results.add(this.insert(item, session));
    }
    return items;
  }

  @Override
  public void delete(E item, DbSession session) {
    Preconditions.checkNotNull(item.getKey(), "Dto does not have a valid Key");
    deleteByKey(item.getKey(), session);
  }

  @Override
  public void delete(Collection<E> items, DbSession session) {
    for (E item : items) {
      delete(item, session);
    }
  }

  @Override
  public void deleteByKey(K key, DbSession session) {
    Preconditions.checkNotNull(key, "cannot delete Item with null key");
    doDeleteByKey(key, session);
    if (hasIndex()) {
      session.enqueue(new KeyIndexAction<K>(this.getIndexType(), IndexAction.Method.DELETE, key));
    }
  }

  protected void enqueueUpdate(Object nestedItem, K key, DbSession session) {
    if (hasIndex()) {
      session.enqueue(new EmbeddedIndexAction<K>(
        this.getIndexType(), IndexAction.Method.UPDATE, nestedItem, key));
    }
  }

  public void enqueueDelete(Object nestedItem, K key, DbSession session) {
    if (hasIndex()) {
      session.enqueue(new EmbeddedIndexAction<K>(
        this.getIndexType(), IndexAction.Method.DELETE, nestedItem, key));
    }
  }

  public void enqueueInsert(Object nestedItem, K key, DbSession session) {
    if (hasIndex()) {
      session.enqueue(new EmbeddedIndexAction<K>(
        this.getIndexType(), IndexAction.Method.INSERT, nestedItem, key));
    }
  }
}
