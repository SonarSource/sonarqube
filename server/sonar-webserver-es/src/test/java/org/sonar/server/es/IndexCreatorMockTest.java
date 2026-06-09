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
package org.sonar.server.es;

import java.util.Map;
import org.junit.Test;
import org.sonar.server.es.metadata.EsDbCompatibility;
import org.sonar.server.es.metadata.MetadataIndex;
import org.sonar.server.es.metadata.MetadataIndexDefinition;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IndexCreatorMockTest {

  private final EsClient client = mock(EsClient.class, RETURNS_DEEP_STUBS);
  private final IndexDefinitions definitions = mock(IndexDefinitions.class);
  private final MetadataIndexDefinition metadataIndexDefinition = mock(MetadataIndexDefinition.class);
  private final MetadataIndex metadataIndex = mock(MetadataIndex.class);
  private final EsDbCompatibility esDbCompatibility = mock(EsDbCompatibility.class);
  private final IndexCreator underTest = new IndexCreator(client, definitions, metadataIndexDefinition,
    metadataIndex, esDbCompatibility);

  @Test
  public void stop_does_nothing() {
    underTest.stop();

    verify(client, never()).deleteIndexV2((String) any());
  }

  @Test
  public void start_skips_creation_when_metadata_exists_and_no_other_indices_defined() {
    // metadata index already exists → ensureWritable path
    when(client.indexExistsV2(any())).thenReturn(true);
    // No additional indices defined
    when(definitions.getIndices()).thenReturn(Map.of());
    when(esDbCompatibility.hasSameDbVendor()).thenReturn(true);
    // Settings response: not read-only
    when(client.getSettingsV2(any()).get(any()).settings()).thenReturn(null);

    underTest.start();

    // No createIndexV2 because index already existed
    verify(client, never()).createIndexV2(any());
  }
}