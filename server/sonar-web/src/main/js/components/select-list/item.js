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
import Checkbox from '../controls/Checkbox';

export default class Item extends React.PureComponent {
  static propTypes = {
    item: PropTypes.any.isRequired,
    renderItem: PropTypes.func.isRequired,
    selectItem: PropTypes.func.isRequired,
    deselectItem: PropTypes.func.isRequired
  };

  onCheck = checked => {
    if (checked) {
      this.props.selectItem(this.props.item);
    } else {
      this.props.deselectItem(this.props.item);
    }
  };

  render() {
    const renderedItem = this.props.renderItem(this.props.item);
    return (
      <li className="panel panel-vertical">
        <div className="display-inline-block text-middle spacer-right">
          <Checkbox checked={!!this.props.item.selected} onCheck={this.onCheck} />
        </div>
        <div
          className="display-inline-block text-middle"
          dangerouslySetInnerHTML={{ __html: renderedItem }}
        />
      </li>
    );
  }
}
