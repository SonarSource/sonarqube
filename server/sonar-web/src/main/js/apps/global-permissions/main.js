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
import $ from 'jquery';
import React from 'react';
import PermissionsList from './permissions-list';
import { translate } from '../../helpers/l10n';

export default React.createClass({
  getInitialState() {
    return { ready: false, permissions: [] };
  },

  componentDidMount() {
    this.requestPermissions();
  },

  requestPermissions() {
    const url = `${window.baseUrl}/api/permissions/search_global_permissions`;
    $.get(url).done(r => {
      this.setState({ ready: true, permissions: r.permissions });
    });
  },

  renderSpinner () {
    if (this.state.ready) {
      return null;
    }
    return <i className="spinner"/>;
  },

  render() {
    return (
        <div className="page">
          <header id="global-permissions-header" className="page-header">
            <h1 className="page-title">{translate('global_permissions.page')}</h1>
            {this.renderSpinner()}
            <p className="page-description">{translate('global_permissions.page.description')}</p>
          </header>
          <PermissionsList ready={this.state.ready} permissions={this.state.permissions}/>
        </div>
    );
  }
});
