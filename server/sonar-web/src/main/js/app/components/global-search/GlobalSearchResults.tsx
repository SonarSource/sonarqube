/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { ItemDivider, ItemHeader } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import GlobalSearchShowMore from './GlobalSearchShowMore';
import { ComponentResult, More, Results, sortQualifiers } from './utils';

export interface Props {
  query: string;
  loadingMore?: string;
  more: More;
  onMoreClick: (qualifier: string) => void;
  onSelect: (componentKey: string) => void;
  renderNoResults: () => React.ReactElement;
  renderResult: (component: ComponentResult) => React.ReactNode;
  results: Results;
  selected?: string;
}

export default function GlobalSearchResults(props: Props): React.ReactElement<Props> {
  const qualifiers = Object.keys(props.results);
  const renderedComponents: React.ReactNode[] = [];
  const allowMore = props.query.length !== 1;

  sortQualifiers(qualifiers).forEach((qualifier) => {
    const components = props.results[qualifier];

    if (components?.length) {
      const more = props.more[qualifier];

      renderedComponents.push(
        <li key={`group-${qualifier}`}>
          <ul key={`header-${qualifier}`} aria-labelledby={translate('qualifiers', qualifier)}>
            <ItemHeader>
              <p id={translate('qualifiers', qualifier)}>{translate('qualifiers', qualifier)}</p>
            </ItemHeader>
            {components.map((component) => props.renderResult(component))}
            {more !== undefined && more > 0 && (
              <GlobalSearchShowMore
                allowMore={allowMore}
                key={`more-${qualifier}`}
                loadingMore={props.loadingMore}
                onMoreClick={props.onMoreClick}
                onSelect={props.onSelect}
                qualifier={qualifier}
                selected={props.selected === `qualifier###${qualifier}`}
              />
            )}
            <ItemDivider />
          </ul>
        </li>,
      );
    }
  });

  return renderedComponents.length > 0 ? <>{renderedComponents}</> : props.renderNoResults();
}
