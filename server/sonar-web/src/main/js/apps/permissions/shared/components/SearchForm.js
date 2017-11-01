/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import PropTypes from 'prop-types';
import RadioToggle from '../../../../components/controls/RadioToggle';
import SearchBox from '../../../../components/controls/SearchBox';
import { translate, translateWithParameters } from '../../../../helpers/l10n';

export default function SearchForm(props) {
  const filterOptions = [
    { value: 'all', label: translate('all') },
    { value: 'users', label: translate('users.page') },
    { value: 'groups', label: translate('user_groups.page') }
  ];

  return (
    <div>
      <RadioToggle
        name="users-or-groups"
        onCheck={props.onFilter}
        options={filterOptions}
        value={props.filter}
      />

      <div className="display-inline-block spacer-left">
        <SearchBox
          minLength={3}
          onChange={props.onSearch}
          placeholder={translate('search_verb')}
          value={props.query}
        />
      </div>
    </div>
  );
}
