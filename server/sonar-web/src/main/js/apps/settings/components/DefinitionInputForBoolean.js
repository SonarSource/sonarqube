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
import RadioToggle from '../../../components/controls/RadioToggle';

export default class DefinitionInputForBoolean extends React.Component {
  static propTypes = {
    definition: React.PropTypes.object.isRequired
  };

  render () {
    const { definition } = this.props;

    const options = [
      { value: '', label: 'Default' },
      { value: 'true', label: 'True' },
      { value: 'false', label: 'False' }
    ];

    return (
        <RadioToggle
            options={options}
            value=""
            name={'settings_' + definition.key}
            onCheck={() => true}/>
    );
  }
}
