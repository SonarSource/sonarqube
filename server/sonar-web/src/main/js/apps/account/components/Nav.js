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
import { IndexLink } from 'react-router';

import UserCard from './UserCard';
import { translate } from '../../../helpers/l10n';

const Nav = ({ user }) => (
    <header className="account-header">
      <UserCard user={user}/>

      <nav className="account-nav clearfix">
        <ul className="nav navbar-nav nav-tabs">
          <li>
            <IndexLink to="/" activeClassName="active">
              <i className="icon-home"/>
            </IndexLink>
          </li>
          <li>
            <a
                className={window.location.pathname === `${window.baseUrl}/account/issues` && 'active'}
                href={`${window.baseUrl}/account/issues`}>
              {translate('issues.page')}
            </a>
          </li>
          <li>
            <IndexLink to="projects" activeClassName="active">
              {translate('my_account.projects')}
            </IndexLink>
          </li>
          <li>
            <IndexLink to="notifications" activeClassName="active">
              {translate('my_account.notifications')}
            </IndexLink>
          </li>
          <li>
            <IndexLink to="security" activeClassName="active">
              {translate('my_account.security')}
            </IndexLink>
          </li>
        </ul>
      </nav>
    </header>
);

export default Nav;
