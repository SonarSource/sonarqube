/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

export type FilterOption = 'all' | 'users' | 'groups';
interface Props {
  filter: FilterOption;
  onFilter: (value: FilterOption) => void;
  onSearch: (value: string) => void;
  query: string;
}

export default function SearchForm(props: Props) {
  const filterOptions = [
    { value: 'all', label: translate('all') },
    { value: 'users', label: translate('users.page') },
    { value: 'groups', label: translate('user_groups.page') },
  ];

  return (
    <div className="sw-flex sw-flex-row">
      <ToggleButton onChange={props.onFilter} options={filterOptions} value={props.filter} />

      <div className="sw-flex-1 sw-ml-2">
        <InputSearch
          minLength={3}
          onChange={props.onSearch}
          placeholder={translate('search.search_for_users_or_groups')}
          value={props.query}
        />
      </div>
    </div>
  );
}
