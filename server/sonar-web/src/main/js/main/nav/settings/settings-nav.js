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
import classNames from 'classnames';
import some from 'lodash/some';
import LinksMixin from '../links-mixin';
import { translate } from '../../../helpers/l10n';

export default React.createClass({
  mixins: [LinksMixin],

  getDefaultProps() {
    return { extensions: [] };
  },

  isSomethingActive(urls) {
    const path = window.location.pathname;
    return some(urls, url => path.indexOf(window.baseUrl + url) === 0);
  },

  isSecurityActive() {
    const urls = ['/users', '/groups', '/roles/global', '/permission_templates'];
    return this.isSomethingActive(urls);
  },

  isProjectsActive() {
    const urls = ['/projects_admin', '/background_tasks'];
    return this.isSomethingActive(urls);
  },

  isSystemActive() {
    const urls = ['/updatecenter', '/system'];
    return this.isSomethingActive(urls);
  },

  render() {
    const isSecurity = this.isSecurityActive();
    const isProjects = this.isProjectsActive();
    const isSystem = this.isSystemActive();

    const securityClassName = classNames('dropdown', { active: isSecurity });
    const projectsClassName = classNames('dropdown', { active: isProjects });
    const systemClassName = classNames('dropdown', { active: isSystem });
    const configurationClassNames = classNames('dropdown', {
      active: !isSecurity && !isProjects && !isSystem
    });

    return (
        <div className="container">
          <ul className="nav navbar-nav nav-crumbs">
            {this.renderLink('/settings', translate('layout.settings'))}
          </ul>

          <ul className="nav navbar-nav nav-tabs">
            <li className={configurationClassNames}>
              <a className="dropdown-toggle" data-toggle="dropdown" href="#">
                {translate('sidebar.project_settings')}
                {' '}
                <i className="icon-dropdown"></i>
              </a>
              <ul className="dropdown-menu">
                {this.renderLink('/settings', translate('settings.page'), url => window.location.pathname === url)}
                {this.renderLink('/settings/licenses', translate('property.category.licenses'))}
                {this.renderLink('/settings/encryption', translate('property.category.security.encryption'))}
                {this.renderLink('/settings/server_id', translate('property.category.server_id'))}
                {this.renderLink('/metrics', 'Custom Metrics')}
                {this.renderLink('/admin_dashboards', translate('default_dashboards.page'))}
                {this.props.extensions.map(e => this.renderLink(e.url, e.name))}
              </ul>
            </li>

            <li className={securityClassName}>
              <a className="dropdown-toggle" data-toggle="dropdown" href="#">
                {translate('sidebar.security')}
                {' '}
                <i className="icon-dropdown"></i>
              </a>
              <ul className="dropdown-menu">
                {this.renderLink('/users', translate('users.page'))}
                {this.renderLink('/groups', translate('user_groups.page'))}
                {this.renderLink('/roles/global',
                    translate('global_permissions.page'))}
                {this.renderLink('/permission_templates',
                    translate('permission_templates'))}
              </ul>
            </li>

            <li className={projectsClassName}>
              <a className="dropdown-toggle" data-toggle="dropdown" href="#">
                {translate('sidebar.projects')}
                {' '}
                <i className="icon-dropdown"></i>
              </a>
              <ul className="dropdown-menu">
                {this.renderLink('/projects_admin', 'Management')}
                {this.renderLink('/background_tasks',
                    translate('background_tasks.page'))}
              </ul>
            </li>

            <li className={systemClassName}>
              <a className="dropdown-toggle" data-toggle="dropdown" href="#">
                {translate('sidebar.system')}
                {' '}
                <i className="icon-dropdown"></i>
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
