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
import renderInput from './renderInput';
import { translate } from '../../../../helpers/l10n';
import { getSettingValue } from '../../utils';

export default class MultiValueInput extends React.Component {
  prepareSetting (value) {
    const { setting } = this.props;
    const newDefinition = { ...setting.definition, multiValues: false };
    return {
      ...setting,
      value,
      definition: newDefinition,
      values: undefined
    };
  }

  render () {
    const { setting } = this.props;

    const values = getSettingValue(setting) || [''];

    return (
        <div>
          <ul>
            {values.map((value, index) => (
                <li key={index} className="spacer-bottom">
                  {renderInput(this.prepareSetting(value))}
                </li>
            ))}
          </ul>
        </div>
    );
  }
}
