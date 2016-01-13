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

import PermissionsHeader from './permissions-header';
import Project from './project';


export default React.createClass({
  propTypes: {
    projects: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    permissions: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    permissionTemplates: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    refresh: React.PropTypes.func.isRequired
  },

  render() {
    let projects = this.props.projects.map(p => {
      return <Project
          key={p.id}
          project={p}
          permissionTemplates={this.props.permissionTemplates}
          refresh={this.props.refresh}/>;
    });
    let className = classNames('data zebra', { 'new-loading': !this.props.ready });
    return (
        <table id="projects" className={className}>
          <PermissionsHeader permissions={this.props.permissions}/>
          <tbody>{projects}</tbody>
        </table>
    );
  }
});
