/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.component.index;

import java.util.List;
import java.util.Optional;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.sonar.core.util.stream.MoreCollectors;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static org.sonar.server.component.index.ComponentIndexDefinition.FIELD_NAME;

public class ComponentHit {

  private final String uuid;
  private final Optional<String> highlightedText;

  public ComponentHit(String uuid) {
    this.uuid = uuid;
    highlightedText = Optional.empty();
  }

  public ComponentHit(SearchHit hit) {
    this.uuid = hit.getId();
    this.highlightedText = getHighlightedText(hit);
  }

  private static Optional<String> getHighlightedText(SearchHit hit) {
    return ofNullable(hit.getHighlightFields())
      .flatMap(fields -> ofNullable(fields.get(FIELD_NAME)))
      .flatMap(field -> ofNullable(field.getFragments()))
      .flatMap(fragments -> stream(fragments).findFirst())
      .map(Text::string);
  }

  public String getUuid() {
    return uuid;
  }

  public static List<ComponentHit> fromSearchHits(SearchHit... hits) {
    return stream(hits)
      .map(ComponentHit::new)
      .collect(MoreCollectors.toList(hits.length));
  }

  public Optional<String> getHighlightedText() {
    return highlightedText;
  }
}
