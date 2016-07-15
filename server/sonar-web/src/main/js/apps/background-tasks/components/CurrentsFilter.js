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
 /* @flow */
import React from 'react';

import Checkbox from '../../../components/controls/Checkbox';
import { CURRENTS } from '../constants';

const CurrentsFilter = ({ value, onChange } : { value: ?string, onChange: any }) => {
  function handleChange (value) {
    const newValue = value ? CURRENTS.ONLY_CURRENTS : CURRENTS.ALL;
    onChange(newValue);
  }

  function handleLabelClick (e) {
    const newValue = value === CURRENTS.ALL ? CURRENTS.ONLY_CURRENTS : CURRENTS.ALL;

    e.preventDefault();
    onChange(newValue);
  }

  const checked = value === CURRENTS.ONLY_CURRENTS;

  return (
      <div className="bt-search-form-field">
        <Checkbox
            checked={checked}
            onCheck={handleChange}/>
        &nbsp;
        <label
            onClick={handleLabelClick}
            style={{ cursor: 'pointer' }}>Yes</label>
      </div>
  );
};

export default CurrentsFilter;
