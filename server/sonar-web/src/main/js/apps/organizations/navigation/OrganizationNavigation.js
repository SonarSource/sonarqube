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
// @flow
import React from 'react';
import { Link, IndexLink } from 'react-router';
import type { Organization } from '../../../store/organizations/duck';
import { translate } from '../../../helpers/l10n';

export default class OrganizationNavigation extends React.Component {
  props: {
    organization: Organization
  };

  render () {
    const { organization } = this.props;

    const adminActive = window.location.pathname.endsWith(`organization/${organization.key}/edit`);

    return (
        <nav className="navbar navbar-context page-container" id="context-navigation">
          <div className="navbar-context-inner">
            <div className="container">
              <ul className="nav navbar-nav nav-crumbs">
                <li>
                  <Link to={`/organizations/${organization.key}`} className={adminActive ? 'active': ''}>
                    {organization.name}
                  </Link>
                </li>
              </ul>

              <ul className="nav navbar-nav nav-tabs">
                <li>
                  <IndexLink to={`/organizations/${organization.key}`} activeClassName="active">
                    <i className="icon-home"/>
                  </IndexLink>
                </li>
                <li>
                  <a className="dropdown-toggle navbar-admin-link" data-toggle="dropdown" href="#">
                    {translate('layout.settings')}&nbsp;<i className="icon-dropdown"/>
                  </a>
                  <ul className="dropdown-menu">
                    <li>
                      <Link to={`/organizations/${organization.key}/edit`} activeClassName="active">
                        {translate('edit')}
                      </Link>
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
