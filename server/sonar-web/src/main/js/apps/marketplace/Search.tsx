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
import * as React from 'react';
import { debounce } from 'lodash';
import RadioToggle from '../../components/controls/RadioToggle';
import { Query } from './utils';
import { translate } from '../../helpers/l10n';

interface Props {
  query: Query;
  updateCenterActive: boolean;
  updateQuery: (newQuery: Partial<Query>) => void;
}

interface State {
  search?: string;
}

export default class Search extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { search: props.query.search };
    this.updateSearch = debounce(this.updateSearch, 250);
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.query.search !== this.state.search) {
      this.setState({ search: nextProps.query.search });
    }
  }

  handleSearch = (e: React.SyntheticEvent<HTMLInputElement>) => {
    const search = e.currentTarget.value;
    this.setState({ search });
    this.updateSearch(search);
  };

  handleFilterChange = (filter: string) => this.props.updateQuery({ filter });

  updateSearch = (search: string) => this.props.updateQuery({ search });

  render() {
    const { query, updateCenterActive } = this.props;
    const radioOptions = [
      { label: translate('marketplace.all'), value: 'all' },
      {
        disabled: !updateCenterActive,
        label: translate('marketplace.updates_only'),
        tooltip: !updateCenterActive ? translate('marketplace.not_activated') : undefined,
        value: 'updates'
      }
    ];
    return (
      <div id="marketplace-search" className="panel panel-vertical bordered-bottom spacer-bottom">
        <div className="display-inline-block text-top nowrap abs-width-150 spacer-right">
          <RadioToggle
            name="marketplace-filter"
            onCheck={this.handleFilterChange}
            options={radioOptions}
            value={query.filter}
          />
        </div>
        <div className="search-box display-inline-block text-top">
          <button className="search-box-submit button-clean">
            <i className="icon-search" />
          </button>
          <input
            onChange={this.handleSearch}
            value={this.state.search}
            className="search-box-input"
            type="search"
            name="search"
            placeholder={translate('search_verb')}
            maxLength={100}
            autoComplete="off"
          />
        </div>
      </div>
    );
  }
}
