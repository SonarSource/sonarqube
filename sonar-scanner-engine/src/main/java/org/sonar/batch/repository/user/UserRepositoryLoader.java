/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.repository.user;

import org.apache.commons.io.IOUtils;

import org.sonar.batch.cache.WSLoaderResult;
import org.sonar.batch.cache.WSLoader;

import javax.annotation.Nullable;

import org.apache.commons.lang.mutable.MutableBoolean;
import com.google.common.collect.Lists;
import com.google.common.base.Joiner;
import org.sonar.batch.util.BatchUtils;
import org.sonar.scanner.protocol.input.ScannerInput;
import com.google.common.base.Function;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class UserRepositoryLoader {
  private final WSLoader wsLoader;

  public UserRepositoryLoader(WSLoader wsLoader) {
    this.wsLoader = wsLoader;
  }

  public ScannerInput.User load(String userLogin) {
    return load(userLogin, null);
  }

  public ScannerInput.User load(String userLogin, @Nullable MutableBoolean fromCache) {
    InputStream is = loadQuery(new UserEncodingFunction().apply(userLogin), fromCache);
    return parseUser(is);
  }

  public Collection<ScannerInput.User> load(List<String> userLogins) {
    return load(userLogins, null);
  }

  /**
   * Not cache friendly. Should not be used if a cache hit is expected.
   */
  public Collection<ScannerInput.User> load(List<String> userLogins, @Nullable MutableBoolean fromCache) {
    if (userLogins.isEmpty()) {
      return Collections.emptyList();
    }
    InputStream is = loadQuery(Joiner.on(',').join(Lists.transform(userLogins, new UserEncodingFunction())), fromCache);

    return parseUsers(is);
  }

  private InputStream loadQuery(String loginsQuery, @Nullable MutableBoolean fromCache) {
    WSLoaderResult<InputStream> result = wsLoader.loadStream("/batch/users?logins=" + loginsQuery);
    if (fromCache != null) {
      fromCache.setValue(result.isFromCache());
    }
    return result.get();
  }

  private static class UserEncodingFunction implements Function<String, String> {
    @Override
    public String apply(String input) {
      return BatchUtils.encodeForUrl(input);
    }
  }

  private static ScannerInput.User parseUser(InputStream is) {
    try {
      return ScannerInput.User.parseDelimitedFrom(is);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to get user details from server", e);
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  private static Collection<ScannerInput.User> parseUsers(InputStream is) {
    List<ScannerInput.User> users = new ArrayList<>();

    try {
      ScannerInput.User user = ScannerInput.User.parseDelimitedFrom(is);
      while (user != null) {
        users.add(user);
        user = ScannerInput.User.parseDelimitedFrom(is);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to get user details from server", e);
    } finally {
      IOUtils.closeQuietly(is);
    }

    return users;
  }

}
