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

import * as React from 'react';
import Link from '../../../components/common/Link';
import ClockIcon from '../../../components/icons/ClockIcon';
import FavoriteIcon from '../../../components/icons/FavoriteIcon';
import QualifierIcon from '../../../components/icons/QualifierIcon';
import { SafeHTMLInjection } from '../../../helpers/sanitize';
import { getComponentOverviewUrl } from '../../../helpers/urls';
import { ComponentResult } from './utils';

interface Props {
  component: ComponentResult;
  innerRef: (componentKey: string, node: HTMLElement | null) => void;
  onClose: () => void;
  onSelect: (componentKey: string) => void;
  selected: boolean;
}
export default class SearchResult extends React.PureComponent<Props> {
  doSelect = () => {
    this.props.onSelect(this.props.component.key);
  };

  render() {
    const { component } = this.props;

    const to = getComponentOverviewUrl(component.key, component.qualifier);

    return (
      <li key={component.key} ref={(node) => this.props.innerRef(component.key, node)}>
        <Link
          className={this.props.selected ? 'hover' : undefined}
          data-key={component.key}
          onClick={this.props.onClose}
          onFocus={this.doSelect}
          to={to}
        >
          <div className="navbar-search-item-link little-padded-top" onMouseEnter={this.doSelect}>
            <div className="display-flex-center">
              <span className="navbar-search-item-icons little-spacer-right">
                {component.isFavorite && <FavoriteIcon favorite={true} size={12} />}
                {!component.isFavorite && component.isRecentlyBrowsed && <ClockIcon size={12} />}
                <QualifierIcon className="little-spacer-right" qualifier={component.qualifier} />
              </span>

              {component.match ? (
                <SafeHTMLInjection htmlAsString={component.match}>
                  <span className="navbar-search-item-match" />
                </SafeHTMLInjection>
              ) : (
                <span className="navbar-search-item-match">{component.name}</span>
              )}
            </div>

            <div className="navbar-search-item-right text-muted-2">{component.key}</div>
          </div>
        </Link>
      </li>
    );
  }
}
