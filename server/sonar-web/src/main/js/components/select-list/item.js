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
import Checkbox from '../shared/checkbox';

export default React.createClass({
  propTypes: {
    item: React.PropTypes.any.isRequired,
    renderItem: React.PropTypes.func.isRequired,
    selectItem: React.PropTypes.func.isRequired,
    deselectItem: React.PropTypes.func.isRequired
  },

  onCheck(checked) {
    if (checked) {
      this.props.selectItem(this.props.item);
    } else {
      this.props.deselectItem(this.props.item);
    }
  },

  render() {
    let renderedItem = this.props.renderItem(this.props.item);
    return (
        <li className="panel panel-vertical">
          <div className="display-inline-block text-middle spacer-right">
            <Checkbox onCheck={this.onCheck} initiallyChecked={!!this.props.item.selected}/>
          </div>
          <div className="display-inline-block text-middle" dangerouslySetInnerHTML={{ __html: renderedItem }}/>
        </li>
    );
  }
});
