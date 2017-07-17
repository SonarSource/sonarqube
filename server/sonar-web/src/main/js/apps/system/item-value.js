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
import ItemBoolean from './item-boolean';
import ItemObject from './item-object';
import ItemLogLevel from './item-log-level';

export default class ItemValue extends React.PureComponent {
  render() {
    if (this.props.name === 'Logs Level') {
      return <ItemLogLevel value={this.props.value} />;
    }

    const rawValue = this.props.value;
    let formattedValue;
    switch (typeof this.props.value) {
      case 'boolean':
        formattedValue = <ItemBoolean value={rawValue} />;
        break;
      case 'object':
        formattedValue = <ItemObject value={rawValue} />;
        break;
      default:
        formattedValue = (
          <code>
            {rawValue}
          </code>
        );
    }
    return formattedValue;
  }
}
