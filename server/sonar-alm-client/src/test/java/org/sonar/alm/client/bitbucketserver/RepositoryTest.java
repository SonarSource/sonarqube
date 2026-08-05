/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.alm.client.bitbucketserver;

import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryTest {

  @Test
  public void getSelfHref_whenLinksIsNull_returnsNull() {
    Repository repository = new Repository("slug", "name", 1L, new Project("key", "name", 2L));

    assertThat(repository.getSelfHref()).isNull();
  }

  @Test
  public void getSelfHref_whenSelfLinksIsNull_returnsNull() {
    Repository repository = new Repository("slug", "name", 1L, new Project("key", "name", 2L))
      .setLinks(new Repository.Links(null));

    assertThat(repository.getSelfHref()).isNull();
  }

  @Test
  public void getSelfHref_whenSelfLinksIsEmpty_returnsNull() {
    Repository repository = new Repository("slug", "name", 1L, new Project("key", "name", 2L))
      .setLinks(new Repository.Links(List.of()));

    assertThat(repository.getSelfHref()).isNull();
  }

  @Test
  public void getSelfHref_whenSelfLinksHasEntries_returnsFirstHref() {
    Repository repository = new Repository("slug", "name", 1L, new Project("key", "name", 2L))
      .setLinks(new Repository.Links(List.of(new Repository.Link("https://bitbucket-server.example.com/browse"))));

    assertThat(repository.getSelfHref()).isEqualTo("https://bitbucket-server.example.com/browse");
  }

}
