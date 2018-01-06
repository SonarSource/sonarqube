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
import React from 'react';
import PropTypes from 'prop-types';
import PropertySetInput from './PropertySetInput';
import MultiValueInput from './MultiValueInput';
import PrimitiveInput from './PrimitiveInput';
import { TYPE_PROPERTY_SET } from '../../constants';

export default class Input extends React.PureComponent {
  static propTypes = {
    setting: PropTypes.object.isRequired,
    value: PropTypes.any,
    onChange: PropTypes.func.isRequired
  };

  render() {
    const { definition } = this.props.setting;

    if (definition.multiValues) {
      return <MultiValueInput {...this.props} />;
    }

    if (definition.type === TYPE_PROPERTY_SET) {
      return <PropertySetInput {...this.props} />;
    }

    return <PrimitiveInput {...this.props} />;
  }
}
