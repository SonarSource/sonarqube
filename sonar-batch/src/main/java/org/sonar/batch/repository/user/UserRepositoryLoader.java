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
package org.sonar.batch.repository.user;

import org.sonar.batch.cache.WSLoaderResult;

import org.sonar.batch.cache.WSLoader;

import javax.annotation.Nullable;

import org.apache.commons.lang.mutable.MutableBoolean;
import com.google.common.collect.Lists;
import com.google.common.base.Joiner;
import org.sonar.batch.util.BatchUtils;
import com.google.common.io.ByteSource;
import com.google.common.base.Function;
import org.sonar.batch.protocol.input.BatchInput;

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

  public BatchInput.User load(String userLogin) {
    return load(userLogin, null);
  }
  
  public BatchInput.User load(String userLogin, @Nullable MutableBoolean fromCache) {
    ByteSource byteSource = loadQuery(new UserEncodingFunction().apply(userLogin), fromCache);
    return parseUser(byteSource);
  }

  public Collection<BatchInput.User> load(List<String> userLogins) {
    return load(userLogins, null);
  }
  
  /**
   * Not cache friendly. Should not be used if a cache hit is expected.
   */
  public Collection<BatchInput.User> load(List<String> userLogins, @Nullable MutableBoolean fromCache) {
    if (userLogins.isEmpty()) {
      return Collections.emptyList();
    }
    ByteSource byteSource = loadQuery(Joiner.on(',').join(Lists.transform(userLogins, new UserEncodingFunction())), fromCache);

    return parseUsers(byteSource);
  }

  private ByteSource loadQuery(String loginsQuery, @Nullable MutableBoolean fromCache) {
    WSLoaderResult<ByteSource> result = wsLoader.loadSource("/scanner/users?logins=" + loginsQuery);
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

  private static BatchInput.User parseUser(ByteSource input) {
    try (InputStream is = input.openStream()) {
      return BatchInput.User.parseDelimitedFrom(is);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to get user details from server", e);
    }
  }

  private static Collection<BatchInput.User> parseUsers(ByteSource input) {
    List<BatchInput.User> users = new ArrayList<>();

    try (InputStream is = input.openStream()) {
      BatchInput.User user = BatchInput.User.parseDelimitedFrom(is);
      while (user != null) {
        users.add(user);
        user = BatchInput.User.parseDelimitedFrom(is);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to get user details from server", e);
    }

    return users;
  }

}
