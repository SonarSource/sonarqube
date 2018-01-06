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
// @flow
import React from 'react';
import SearchShowMore from './SearchShowMore';
import { sortQualifiers } from './utils';
/*:: import type { Component, More, Results } from './utils'; */
import { translate } from '../../../helpers/l10n';

/*::
type Props = {|
  allowMore: boolean,
  loadingMore: ?string,
  more: More,
  onMoreClick: string => void,
  onSelect: string => void,
  renderNoResults: () => React.Element<*>,
  renderResult: Component => React.Element<*>,
  results: Results,
  selected: ?string
|};
*/

export default class SearchResults extends React.PureComponent {
  /*:: props: Props; */

  render() {
    const qualifiers = Object.keys(this.props.results);
    const renderedComponents = [];

    sortQualifiers(qualifiers).forEach(qualifier => {
      const components = this.props.results[qualifier];

      if (components.length > 0 && renderedComponents.length > 0) {
        renderedComponents.push(<li key={`divider-${qualifier}`} className="divider" />);
      }

      if (components.length > 0) {
        renderedComponents.push(
          <li key={`header-${qualifier}`} className="dropdown-header">
            {translate('qualifiers', qualifier)}
          </li>
        );
      }

      components.forEach(component => renderedComponents.push(this.props.renderResult(component)));

      const more = this.props.more[qualifier];
      if (more != null && more > 0) {
        renderedComponents.push(
          <SearchShowMore
            allowMore={this.props.allowMore}
            key={`more-${qualifier}`}
            loadingMore={this.props.loadingMore}
            onMoreClick={this.props.onMoreClick}
            onSelect={this.props.onSelect}
            qualifier={qualifier}
            selected={this.props.selected === `qualifier###${qualifier}`}
          />
        );
      }
    });

    return renderedComponents.length > 0 ? (
      <ul className="menu">{renderedComponents}</ul>
    ) : (
      this.props.renderNoResults()
    );
  }
}
