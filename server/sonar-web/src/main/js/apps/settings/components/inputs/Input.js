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
import shallowCompare from 'react-addons-shallow-compare';
import PropertySetInput from './PropertySetInput';
import MultiValueInput from './MultiValueInput';
import renderInput from './renderInput';
import { TYPE_PROPERTY_SET } from '../../constants';

export default class Input extends React.Component {
  static propTypes = {
    setting: React.PropTypes.object.isRequired,
    onChange: React.PropTypes.func.isRequired
  };

  shouldComponentUpdate (nextProps, nextState) {
    return shallowCompare(this, nextProps, nextState);
  }

  render () {
    const { setting, onChange } = this.props;

    if (setting.definition.type === TYPE_PROPERTY_SET) {
      return <PropertySetInput setting={setting} onChange={onChange}/>;
    }

    if (setting.definition.multiValues) {
      return <MultiValueInput setting={setting} onChange={onChange}/>;
    }

    return renderInput(setting, onChange);
  }
}
