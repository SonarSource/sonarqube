/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.auth.ldap;

import javax.annotation.Nullable;
import javax.naming.Context;
import javax.naming.NamingException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * @author Evgeny Mandrikov
 */
public final class ContextHelper {

  private static final Logger LOG = Loggers.get(ContextHelper.class);

  private ContextHelper() {
  }

  /**
   * <pre>
   * public void useContextNicely() throws NamingException {
   *   InitialDirContext context = null;
   *   boolean threw = true;
   *   try {
   *     context = new InitialDirContext();
   *     // Some code which does something with the Context and may throw a NamingException
   *     threw = false; // No throwable thrown
   *   } finally {
   *     // Close context
   *     // If an exception occurs, only rethrow it if (threw==false)
   *     close(context, threw);
   *   }
   * }
   * </pre>
   *
   * @param context the {@code Context} object to be closed, or null, in which case this method does nothing
   * @param swallowIOException if true, don't propagate {@code NamingException} thrown by the {@code close} method
   * @throws NamingException if {@code swallowIOException} is false and {@code close} throws a {@code NamingException}.
   */
  public static void close(@Nullable Context context, boolean swallowIOException) throws NamingException {
    if (context == null) {
      return;
    }
    try {
      context.close();
    } catch (NamingException e) {
      if (swallowIOException) {
        LOG.warn("NamingException thrown while closing context.", e);
      } else {
        throw e;
      }
    }
  }

  public static void closeQuietly(@Nullable Context context) {
    try {
      close(context, true);
    } catch (NamingException e) {
      LOG.error("Unexpected NamingException", e);
    }
  }

}
