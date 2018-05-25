/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

package org.sonar.server.platform.db.migration.step;

import java.sql.SQLException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.server.platform.db.migration.es.ElasticsearchClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ElasticsearchChangeTest {

  private ElasticsearchClient client = mock(ElasticsearchClient.class);

  @Test
  public void deleteIndice_call() throws SQLException {
    ArgumentCaptor<String> indices = ArgumentCaptor.forClass(String.class);

    new ElasticsearchChange(client) {
      @Override
      protected void execute(Context context) throws SQLException {
        context.deleteIndice("a", "b", "c");
      }
    }.execute();

    verify(client, times(1)).deleteIndice(indices.capture());
    assertThat(indices.getAllValues()).containsExactly("a", "b", "c");
  }
}
