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
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.Dto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.action.DtoIndexAction;
import org.sonar.server.search.action.EmbeddedIndexAction;
import org.sonar.server.search.action.IndexAction;
import org.sonar.server.search.action.KeyIndexAction;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

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

  protected M mapper(DbSession session) {
    return session.getMapper(mapperClass);
  }

  public E getNullableByKey(DbSession session, K key) {
    return doGetNullableByKey(session, key);
  }

  public final E getByKey(DbSession session, K key) {
    E value = doGetNullableByKey(session, key);
    if (value == null) {
      throw new NotFoundException(String.format("Key '%s' not found", key));
    }
    return value;
  }

  @Override
  public final E update(DbSession session, E item) {
    Date now = new Date(system2.now());
    update(session, item, now);
    return item;
  }

  @Override
  public final E update(DbSession session, E item, E... others) {
    Date now = new Date(system2.now());
    update(session, item, now);
    for (E other : others) {
      update(session, other, now);
    }
    return item;
  }

  @Override
  public final Collection<E> update(DbSession session, Collection<E> items) {
    Date now = new Date(system2.now());
    for (E item : items) {
      update(session, item, now);
    }
    return items;
  }

  private void update(DbSession session, E item, Date now) {
    try {
      item.setUpdatedAt(now);
      doUpdate(session, item);
      if (hasIndex()) {
        session.enqueue(new DtoIndexAction<E>(getIndexType(), IndexAction.Method.UPSERT, item));
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to update item in db: " + item, e);
    }
  }

  @Override
  public final E insert(DbSession session, E item) {
    insert(session, item, new Date(system2.now()));
    return item;
  }

  @Override
  public final Collection<E> insert(DbSession session, Collection<E> items) {
    Date now = new Date(system2.now());
    for (E item : items) {
      insert(session, item, now);
    }
    return items;
  }

  @Override
  public final E insert(DbSession session, E item, E... others) {
    Date now = new Date(system2.now());
    insert(session, item, now);
    for (E other : others) {
      insert(session, other, now);
    }
    return item;
  }

  private void insert(DbSession session, E item, Date now) {
    if (item.getCreatedAt() == null) {
      item.setCreatedAt(now);
    }
    item.setUpdatedAt(now);
    try {
      doInsert(session, item);
      if (hasIndex()) {
        session.enqueue(new DtoIndexAction<E>(getIndexType(), IndexAction.Method.UPSERT, item));
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to insert item in db: " + item, e.getCause());
    }
  }

  @Override
  public final void delete(DbSession session, E item) {
    deleteByKey(session, item.getKey());
  }

  @Override
  public final void delete(DbSession session, E item, E... others) {
    delete(session, item);
    for (E e : others) {
      delete(session, e);
    }
  }

  @Override
  public final void delete(DbSession session, Collection<E> items) {
    for (E item : items) {
      delete(session, item);
    }
  }

  @Override
  public final void deleteByKey(DbSession session, K key) {
    Preconditions.checkNotNull(key, "Missing key");
    try {
      doDeleteByKey(session, key);
      if (hasIndex()) {
        session.enqueue(new KeyIndexAction<K>(getIndexType(), IndexAction.Method.DELETE, key));
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to delete item from db: " + key, e);
    }
  }

  protected final void enqueueUpdate(Object nestedItem, K key, DbSession session) {
    if (hasIndex()) {
      session.enqueue(new EmbeddedIndexAction<K>(
        this.getIndexType(), IndexAction.Method.UPSERT, key, nestedItem));
    }
  }

  public void enqueueDelete(Object nestedItem, K key, DbSession session) {
    if (hasIndex()) {
      session.enqueue(new EmbeddedIndexAction<K>(
        this.getIndexType(), IndexAction.Method.DELETE, key, nestedItem));
      session.commit();
    }
  }

  public void enqueueInsert(Object nestedItem, K key, DbSession session) {
    if (hasIndex()) {
      session.enqueue(new EmbeddedIndexAction<K>(
        this.getIndexType(), IndexAction.Method.UPSERT, key, nestedItem));
    }
  }

  @Override
  public final void synchronizeAfter(final DbSession session, Date date) {
    for (E dto : this.findAfterDate(session, date)) {
      session.enqueue(new DtoIndexAction<E>(getIndexType(), IndexAction.Method.UPSERT,
        dto));
    }
    session.commit();
  }

  @CheckForNull
  protected abstract E doGetNullableByKey(DbSession session, K key);

  protected Iterable<E> findAfterDate(final DbSession session, Date date) {
    throw notImplemented(this);
  }

  protected E doInsert(DbSession session, E item) {
    throw notImplemented(this);
  }

  protected E doUpdate(DbSession session, E item) {
    throw notImplemented(this);
  }

  protected void doDeleteByKey(DbSession session, K key) {
    throw notImplemented(this);
  }

  private static RuntimeException notImplemented(BaseDao baseDao) {
    throw new IllegalStateException("Not implemented yet for class [" + baseDao.getClass().getSimpleName() + "]");
  }
}
