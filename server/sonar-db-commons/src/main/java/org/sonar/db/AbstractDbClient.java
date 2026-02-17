/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
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
package org.sonar.db;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * AbstractDbClient is used to create a convenience class that aggregates multiple Dao instances into a single injectable
 * object that can also be used to open sessions. This can be used to reduce the number of parameters
 * that have to be injected into a class that uses a lot of Dao instances.
 * If desired, multiple AbstractDbClient subclasses can be created to group different sets of related Dao instances.
 * The traditional pattern adds a getter for each Dao, either using {@link #getDao(Class)} in the getter itself
 * or using {@link #getDao(Class)} to fill in fields in the constructor.
 * The DbClient also delegates to a {@link DBSessions} instance for opening sessions, removing another
 * parameter that would otherwise have to be injected into classes that use the database.
 * Using this class is not required, it is possible to inject Dao instances and DBSessions directly into classes.
 */
public abstract class AbstractDbClient {

  private final Database database;
  private final MyBatis myBatis;
  private final DBSessions dbSessions;
  private final Map<Class<?>, Dao> daoMap;

  protected AbstractDbClient(Database database, MyBatis myBatis, DBSessions dbSessions, Dao... daos) {
    this(database, myBatis, dbSessions, Arrays.asList(daos));
  }

  protected AbstractDbClient(Database database, MyBatis myBatis, DBSessions dbSessions, Collection<Dao> daos) {
    this.database = database;
    this.myBatis = myBatis;
    this.dbSessions = dbSessions;

    var map = new IdentityHashMap<Class<?>, Dao>();
    for (Dao dao : daos) {
      map.put(dao.getClass(), dao);
    }
    daoMap = Collections.unmodifiableMap(map);
  }

  /**
   * Opens a new session from the {@link DBSessions} instance. This is just syntactic sugar
   * to avoid having to inject DBSessions itself.
   */
  public DbSession openSession(boolean batch) {
    return dbSessions.openSession(batch);
  }

  public Database getDatabase() {
    return database;
  }

  private static <K extends Dao> K getDao(Map<Class<?>, Dao> map, Class<K> clazz) {
    return (K) map.get(clazz);
  }

  /**
   * Used by subtypes to find the Dao instances to fill in their fields.
   * This converts from a collection of abstract Dao instances passed to the constructor,
   * to specific subtype instances.
   */
  protected <K extends Dao> K getDao(Class<K> clazz) {
    return getDao(daoMap, clazz);
  }

  // should be removed. Still used by some old DAO in sonar-server

  /**
   * This should be removed because {@link #openSession(boolean)} that goes through {@link DBSessions} should be used instead,
   * but it is still used by some code to call {@link MyBatis#newScrollingSelectStatement(DbSession, String)}.
   * To clean this up, make {@link MyBatis#newScrollingSelectStatement(DbSession, String)} available here
   * directly and remove this method perhaps.
   */
  public MyBatis getMyBatis() {
    return myBatis;
  }
}
