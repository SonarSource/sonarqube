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
import { InputSearch, ToggleButton } from 'design-system';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import { Query } from './utils';

interface Props {
  query: Query;
  updateCenterActive: boolean;
  updateQuery: (newQuery: Partial<Query>) => void;
}

export default class Search extends React.PureComponent<Props> {
  handleSearch = (search: string) => {
    this.props.updateQuery({ search });
  };

  handleFilterChange = (filter: string) => this.props.updateQuery({ filter });

  render() {
    const { query, updateCenterActive } = this.props;
    const radioOptions = [
      { label: translate('marketplace.all'), value: 'all' },
      { label: translate('marketplace.installed'), value: 'installed' },
      {
        disabled: !updateCenterActive,
        label: translate('marketplace.updates_only'),
        tooltip: !updateCenterActive ? translate('marketplace.not_activated') : undefined,
        value: 'updates',
      },
    ];
    return (
      <div className="sw-mt-6 sw-flex sw-gap-6" id="marketplace-search">
        <ToggleButton
          onChange={this.handleFilterChange}
          options={radioOptions}
          value={query.filter}
        />
        <InputSearch
          onChange={this.handleSearch}
          placeholder={translate('marketplace.search')}
          size="large"
          value={query.search}
        />
      </div>
    );
  }
}
