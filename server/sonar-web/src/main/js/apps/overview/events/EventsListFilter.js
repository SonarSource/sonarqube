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
import React from 'react';
import Select from 'react-select';
import { translate } from '../../../helpers/l10n';

const TYPES = ['All', 'Version', 'Alert', 'Profile', 'Other'];

const EventsListFilter = ({ currentFilter, onFilter }) => {
  const handleChange = selected => onFilter(selected.value);

  const options = TYPES.map(type => {
    return {
      value: type,
      label: translate('event.category', type)
    };
  });

  return (
      <Select
          value={currentFilter}
          options={options}
          clearable={false}
          searchable={false}
          onChange={handleChange}
          style={{ width: '125px' }}/>
  );
};

EventsListFilter.propTypes = {
  onFilter: React.PropTypes.func.isRequired,
  currentFilter: React.PropTypes.string.isRequired
};

export default EventsListFilter;
