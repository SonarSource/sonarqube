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

export default {
  propTypes: {
    permission: React.PropTypes.object.isRequired,
    max: React.PropTypes.number.isRequired,
    items: React.PropTypes.array,
    total: React.PropTypes.number,
    refresh: React.PropTypes.func.isRequired
  },

  renderNotDisplayed() {
    const notDisplayedCount = this.props.total - this.props.max;
    return notDisplayedCount > 0 ?
        <span className="note spacer-right" href="#">and {notDisplayedCount} more</span> : null;
  },

  renderItems() {
    const displayed = this.props.items.map(item => {
      return <li key={item.name} className="spacer-left little-spacer-bottom">{this.renderItem(item)}</li>;
    });
    return (
        <ul className="overflow-hidden bordered-left">
          {displayed}
          <li className="spacer-left little-spacer-bottom">
            {this.renderNotDisplayed()}
            {this.renderUpdateLink()}
          </li>
        </ul>
    );
  },

  renderCount() {
    return (
        <ul className="overflow-hidden bordered-left">
          <li className="spacer-left little-spacer-bottom">
            <span className="spacer-right">{this.props.total}</span>
            {this.renderUpdateLink()}
          </li>
        </ul>
    );
  },

  render() {
    return (
        <li className="abs-width-400">
          <div className="pull-left spacer-right">
            <strong>{this.renderTitle()}</strong>
          </div>
          {this.props.items ? this.renderItems() : this.renderCount()}
        </li>
    );
  }
};
