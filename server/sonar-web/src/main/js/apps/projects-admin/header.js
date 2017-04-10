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
import CreateView from './create-view';
import BulkApplyTemplateView from './views/BulkApplyTemplateView';

export default class Header extends React.Component {
  static propTypes = {
    hasProvisionPermission: React.PropTypes.bool.isRequired
  };

  createProject() {
    new CreateView({
      refresh: this.props.refresh,
      organization: this.props.organization
    }).render();
  }

  bulkApplyTemplate() {
    new BulkApplyTemplateView({
      total: this.props.total,
      selection: this.props.selection,
      query: this.props.query,
      qualifier: this.props.qualifier,
      organization: this.props.organization
    }).render();
  }

  renderCreateButton() {
    if (!this.props.hasProvisionPermission) {
      return null;
    }
    return (
      <li>
        <button onClick={this.createProject.bind(this)}>
          Create Project
        </button>
      </li>
    );
  }

  renderBulkApplyTemplateButton() {
    return (
      <li>
        <button onClick={this.bulkApplyTemplate.bind(this)}>
          Bulk Apply Permission Template
        </button>
      </li>
    );
  }

  render() {
    return (
      <header className="page-header">
        <h1 className="page-title">Projects Management</h1>
        <div className="page-actions">
          <ul className="list-inline">
            {this.renderCreateButton()}
            {this.renderBulkApplyTemplateButton()}
          </ul>
        </div>
        <p className="page-description">
          Use this page to delete multiple projects at once, or to provision projects
          {' '}
          if you would like to configure them before the first analysis. Note that once
          {' '}
          a project is provisioned, you have access to perform all project configurations on it.
        </p>
      </header>
    );
  }
}
