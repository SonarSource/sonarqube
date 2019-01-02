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
import * as React from 'react';
import SearchShowMore from './SearchShowMore';
import { sortQualifiers, More, ComponentResult, Results } from './utils';
import { translate } from '../../../helpers/l10n';

export interface Props {
  allowMore: boolean;
  loadingMore?: string;
  more: More;
  onMoreClick: (qualifier: string) => void;
  onSelect: (componentKey: string) => void;
  renderNoResults: () => React.ReactElement<any>;
  renderResult: (component: ComponentResult) => React.ReactNode;
  results: Results;
  selected?: string;
}

export default function SearchResults(props: Props): React.ReactElement<Props> {
  const qualifiers = Object.keys(props.results);
  const renderedComponents: React.ReactNode[] = [];

  sortQualifiers(qualifiers).forEach(qualifier => {
    const components = props.results[qualifier];

    if (components.length > 0 && renderedComponents.length > 0) {
      renderedComponents.push(<li className="divider" key={`divider-${qualifier}`} />);
    }

    if (components.length > 0) {
      renderedComponents.push(
        <li className="menu-header" key={`header-${qualifier}`}>
          {translate('qualifiers', qualifier)}
        </li>
      );
    }

    components.forEach(component => renderedComponents.push(props.renderResult(component)));

    const more = props.more[qualifier];
    if (more !== undefined && more > 0) {
      renderedComponents.push(
        <SearchShowMore
          allowMore={props.allowMore}
          key={`more-${qualifier}`}
          loadingMore={props.loadingMore}
          onMoreClick={props.onMoreClick}
          onSelect={props.onSelect}
          qualifier={qualifier}
          selected={props.selected === `qualifier###${qualifier}`}
        />
      );
    }
  });

  return renderedComponents.length > 0 ? (
    <ul className="menu">{renderedComponents}</ul>
  ) : (
    props.renderNoResults()
  );
}
