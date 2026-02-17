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

/**
 * Wraps the SqlSession from MyBatis which in turn wraps the JDBC Connection.
 * The main purpose of this interface is to be able to enable/disable thread-local caching of a session
 * object (returns the same object from multiple calls to openSession). This is historically
 * done in a servlet filter for the scope of one request.
 */
public interface DBSessions {
  /**
   * Opens a MyBatis session. If caching is enabled, returns the same thread-local session object
   * each time within each thread. Closing the cached session object will reset the transaction but not
   * actually close the underlying session, so it is still usable by the next caller.
   * The caching is thread-local because DbSession is not at all thread-safe.
   */
  DbSession openSession(boolean batch);

  /**
   * Causes all subsequent calls of openSession on this thread to return the same session object,
   * until disableCaching() is called.
   */
  void enableCaching();

  /**
   * Causes all subsequent calls of openSession on this thread to return a new session object,
   * until enableCaching() is called.
   * If caching is currently enabled on this thread, the session object that is being cached will be closed.
   */
  void disableCaching();
}
