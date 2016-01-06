/*
 * SonarQube :: Web
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
import LinksMixin from '../links-mixin';
import { translate } from '../../../helpers/l10n';

export default React.createClass({
  mixins: [LinksMixin],

  getDefaultProps() {
    return { extensions: [] };
  },

  render() {
    return (
        <div className="container">
          <ul className="nav navbar-nav nav-crumbs">
            {this.renderLink('/settings', translate('layout.settings'))}
          </ul>

          <ul className="nav navbar-nav nav-tabs">
            <li className="dropdown">
              <a className="dropdown-toggle" data-toggle="dropdown" href="#">
                {translate('sidebar.project_settings')}&nbsp;<i className="icon-dropdown"></i>
              </a>
              <ul className="dropdown-menu">
                {this.renderLink('/settings', translate('settings.page'))}
                {this.renderLink('/metrics', 'Custom Metrics')}
                {this.renderLink('/admin_dashboards', translate('default_dashboards.page'))}
                {this.props.extensions.map(e => this.renderLink(e.url, e.name))}
              </ul>
            </li>

            <li className="dropdown">
              <a className="dropdown-toggle" data-toggle="dropdown" href="#">
                {translate('sidebar.security')}&nbsp;<i className="icon-dropdown"></i>
              </a>
              <ul className="dropdown-menu">
                {this.renderLink('/users', translate('users.page'))}
                {this.renderLink('/groups', translate('user_groups.page'))}
                {this.renderLink('/roles/global', translate('global_permissions.page'))}
                {this.renderLink('/roles/projects', translate('roles.page'))}
                {this.renderLink('/permission_templates', translate('permission_templates'))}
              </ul>
            </li>

            <li className="dropdown">
              <a className="dropdown-toggle" data-toggle="dropdown" href="#">
                {translate('sidebar.projects')}&nbsp;<i className="icon-dropdown"></i>
              </a>
              <ul className="dropdown-menu">
                {this.renderLink('/projects', 'Management')}
                {this.renderLink('/background_tasks', translate('background_tasks.page'))}
              </ul>
            </li>

            <li className="dropdown">
              <a className="dropdown-toggle" data-toggle="dropdown" href="#">
                {translate('sidebar.system')}&nbsp;<i className="icon-dropdown"></i>
              </a>
              <ul className="dropdown-menu">
                {this.renderLink('/updatecenter', translate('update_center.page'))}
                {this.renderLink('/system', translate('system_info.page'))}
              </ul>
            </li>
          </ul>
        </div>

    );
  }
});
