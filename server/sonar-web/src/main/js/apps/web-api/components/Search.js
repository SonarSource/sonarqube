/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import _ from 'underscore';
import React from 'react';

import DeprecatedBadge from './DeprecatedBadge';
import InternalBadge from './InternalBadge';
import Checkbox from '../../../components/shared/checkbox';

export default class Search extends React.Component {
  constructor (props) {
    super(props);
    this.state = { query: '' };
    this.actuallySearch = _.debounce(this.actuallySearch.bind(this), 250);
  }

  handleSearch (e) {
    const { value } = e.target;
    this.setState({ query: value });
    this.actuallySearch();
  }

  actuallySearch () {
    const { onSearch } = this.props;
    onSearch(this.state.query);
  }

  render () {
    const { showInternal, showOnlyDeprecated, onToggleInternal, onToggleDeprecated } = this.props;

    return (
        <div className="web-api-search">
          <div>
            <i className="icon-search"/>
            <input
                className="spacer-left input-large"
                type="search"
                value={this.state.query}
                placeholder="Search..."
                onChange={this.handleSearch.bind(this)}/>
          </div>

          <div className="big-spacer-top">
            <Checkbox
                initiallyChecked={showInternal}
                onCheck={onToggleInternal}/>
            {' '}
            <span
                style={{ cursor: 'pointer' }}
                onClick={onToggleInternal}>
              <InternalBadge/>
            </span>
          </div>

          <div className="spacer-top">
            <Checkbox
                initiallyChecked={showOnlyDeprecated}
                onCheck={onToggleDeprecated}/>
            {' '}
            <span
                style={{ cursor: 'pointer' }}
                onClick={onToggleDeprecated}>
              Only <DeprecatedBadge/>
            </span>
          </div>
        </div>
    );
  }
}
