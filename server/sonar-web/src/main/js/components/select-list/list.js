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
import Item from './item';

export default class List extends React.PureComponent {
  static propTypes = {
    items: PropTypes.array.isRequired,
    renderItem: PropTypes.func.isRequired,
    getItemKey: PropTypes.func.isRequired,
    selectItem: PropTypes.func.isRequired,
    deselectItem: PropTypes.func.isRequired
  };

  render() {
    const renderedItems = this.props.items.map(item => {
      const key = this.props.getItemKey(item);
      return <Item key={key} {...this.props} item={item} />;
    });
    return (
      <ul>
        {renderedItems}
      </ul>
    );
  }
}
