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
import classNames from 'classnames';
import React from 'react';

import Permission from './permission';


export default React.createClass({
  propTypes: {
    permissions: React.PropTypes.arrayOf(React.PropTypes.object).isRequired
  },

  renderPermissions() {
    return this.props.permissions.map(permission => {
      return <Permission key={permission.key} permission={permission} project={this.props.project}/>;
    });
  },

  render() {
    let className = classNames({ 'new-loading': !this.props.ready });
    return <ul id="global-permissions-list" className={className}>{this.renderPermissions()}</ul>;
  }
});
