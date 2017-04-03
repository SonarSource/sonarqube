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
import QualifierIcon from '../shared/QualifierIcon';

export const TreemapBreadcrumbs = React.createClass({
  propTypes: {
    breadcrumbs: React.PropTypes.arrayOf(
      React.PropTypes.shape({
        key: React.PropTypes.string.isRequired,
        name: React.PropTypes.string.isRequired,
        qualifier: React.PropTypes.string.isRequired
      }).isRequired
    ).isRequired
  },

  handleItemClick(item, e) {
    e.preventDefault();
    this.props.onRectangleClick(item);
  },

  handleReset(e) {
    e.preventDefault();
    this.props.onReset();
  },

  renderHome() {
    return (
      <span className="treemap-breadcrumbs-item">
        <a onClick={this.handleReset} className="icon-home" href="#" />
      </span>
    );
  },

  renderBreadcrumbsItems(b) {
    return (
      <span key={b.key} className="treemap-breadcrumbs-item" title={b.name}>
        <i className="icon-chevron-right" />
        <QualifierIcon qualifier={b.qualifier} />
        <a onClick={this.handleItemClick.bind(this, b)} href="#">{b.name}</a>
      </span>
    );
  },

  render() {
    const breadcrumbs = this.props.breadcrumbs.map(this.renderBreadcrumbsItems);
    return (
      <div className="treemap-breadcrumbs">
        {this.props.breadcrumbs.length ? this.renderHome() : null}
        {breadcrumbs}
      </div>
    );
  }
});
