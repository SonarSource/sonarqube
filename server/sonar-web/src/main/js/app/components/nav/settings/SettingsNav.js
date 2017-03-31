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
import classNames from 'classnames';
import { IndexLink, Link } from 'react-router';
import { connect } from 'react-redux';
import { translate } from '../../../../helpers/l10n';
import { areThereCustomOrganizations } from '../../../../store/rootReducer';

class SettingsNav extends React.Component {
  static defaultProps = {
    extensions: []
  };

  isSomethingActive(urls) {
    const path = window.location.pathname;
    return urls.some(url => path.indexOf(window.baseUrl + url) === 0);
  }

  isSecurityActive() {
    const urls = ['/users', '/groups', '/roles/global', '/permission_templates'];
    return this.isSomethingActive(urls);
  }

  isProjectsActive() {
    const urls = ['/projects_admin', '/background_tasks'];
    return this.isSomethingActive(urls);
  }

  isSystemActive() {
    const urls = ['/updatecenter', '/system'];
    return this.isSomethingActive(urls);
  }

  renderExtension = ({ key, name }) => {
    return (
      <li key={key}>
        <Link to={`/admin/extension/${key}`} activeClassName="active">{name}</Link>
      </li>
    );
  };

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
      <nav className="navbar navbar-context page-container" id="context-navigation">
        <div className="navbar-context-inner">
          <div className="container">
            <ul className="nav navbar-nav nav-crumbs">
              <li>
                <IndexLink to="/settings">
                  {translate('layout.settings')}
                </IndexLink>
              </li>
            </ul>

            <ul className="nav navbar-nav nav-tabs">
              <li className={configurationClassNames}>
                <a
                  className="dropdown-toggle"
                  data-toggle="dropdown"
                  id="settings-navigation-configuration"
                  href="#">
                  {translate('sidebar.project_settings')} <i className="icon-dropdown" />
                </a>
                <ul className="dropdown-menu">
                  <li>
                    <IndexLink to="/settings" activeClassName="active">
                      {translate('settings.page')}
                    </IndexLink>
                  </li>
                  <li>
                    <IndexLink to="/settings/licenses" activeClassName="active">
                      {translate('property.category.licenses')}
                    </IndexLink>
                  </li>
                  <li>
                    <IndexLink to="/settings/encryption" activeClassName="active">
                      {translate('property.category.security.encryption')}
                    </IndexLink>
                  </li>
                  <li>
                    <IndexLink to="/settings/server_id" activeClassName="active">
                      {translate('property.category.server_id')}
                    </IndexLink>
                  </li>
                  <li>
                    <IndexLink to="/metrics" activeClassName="active">
                      Custom Metrics
                    </IndexLink>
                  </li>
                  {this.props.extensions.map(this.renderExtension)}
                </ul>
              </li>

              <li className={securityClassName}>
                <a className="dropdown-toggle" data-toggle="dropdown" href="#">
                  {translate('sidebar.security')} <i className="icon-dropdown" />
                </a>
                <ul className="dropdown-menu">
                  <li>
                    <IndexLink to="/users" activeClassName="active">
                      {translate('users.page')}
                    </IndexLink>
                  </li>
                  {!this.props.customOrganizations &&
                    <li>
                      <IndexLink to="/groups" activeClassName="active">
                        {translate('user_groups.page')}
                      </IndexLink>
                    </li>}
                  {!this.props.customOrganizations &&
                    <li>
                      <IndexLink to="/roles/global" activeClassName="active">
                        {translate('global_permissions.page')}
                      </IndexLink>
                    </li>}
                  {!this.props.customOrganizations &&
                    <li>
                      <IndexLink to="/permission_templates" activeClassName="active">
                        {translate('permission_templates')}
                      </IndexLink>
                    </li>}
                </ul>
              </li>

              <li className={projectsClassName}>
                <a className="dropdown-toggle" data-toggle="dropdown" href="#">
                  {translate('sidebar.projects')} <i className="icon-dropdown" />
                </a>
                <ul className="dropdown-menu">
                  {!this.props.customOrganizations &&
                    <li>
                      <IndexLink to="/projects_admin" activeClassName="active">
                        Management
                      </IndexLink>
                    </li>}
                  <li>
                    <IndexLink to="/background_tasks" activeClassName="active">
                      {translate('background_tasks.page')}
                    </IndexLink>
                  </li>
                </ul>
              </li>

              <li className={systemClassName}>
                <a className="dropdown-toggle" data-toggle="dropdown" href="#">
                  {translate('sidebar.system')} <i className="icon-dropdown" />
                </a>
                <ul className="dropdown-menu">
                  <li>
                    <IndexLink to="/updatecenter" activeClassName="active">
                      {translate('update_center.page')}
                    </IndexLink>
                  </li>
                  <li>
                    <IndexLink to="/system" activeClassName="active">
                      {translate('system_info.page')}
                    </IndexLink>
                  </li>
                </ul>
              </li>
            </ul>
          </div>
        </div>
      </nav>
    );
  }
}

const mapStateToProps = state => ({
  customOrganizations: areThereCustomOrganizations(state)
});

export default connect(mapStateToProps)(SettingsNav);

export const UnconnectedSettingsNav = SettingsNav;
