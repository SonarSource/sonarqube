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
import { getUniqueName } from '../../utils';
import { getSettingValue } from '../../utils';

export default class InputForText extends React.Component {
  static propTypes = {
    setting: React.PropTypes.object.isRequired
  };

  render () {
    const { setting } = this.props;

    return (
        <textarea
            name={getUniqueName(setting.key)}
            className="input-super-large text-top"
            rows="5">
          {getSettingValue(setting)}
          </textarea>
    );
  }
}
