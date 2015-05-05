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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.InputSupplier;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.protocol.input.BatchInput;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class UserRepository {

  private ServerClient serverClient;

  public UserRepository(ServerClient serverClient) {
    this.serverClient = serverClient;
  }

  public Collection<BatchInput.User> loadFromWs(List<String> userLogins) {
    if (userLogins.isEmpty()) {
      return Collections.emptyList();
    }
    InputSupplier<InputStream> request = serverClient.doRequest("/batch/users?logins=" + Joiner.on(',').join(Lists.transform(userLogins, new Function<String, String>() {
      @Override
      public String apply(String input) {
        return ServerClient.encodeForUrl(input);
      }
    })), "GET", null);
    List<BatchInput.User> users = new ArrayList<>();
    try (InputStream is = request.getInput()) {
      BatchInput.User user = BatchInput.User.parseDelimitedFrom(is);
      while (user != null) {
        users.add(user);
        user = BatchInput.User.parseDelimitedFrom(is);
      }
    } catch (HttpDownloader.HttpException e) {
      throw serverClient.handleHttpException(e);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to get user details from server", e);
    }
    return users;
  }

}
