/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import * as React from 'react';
import { NavLink } from 'react-router-dom';
import NavBarTabs from '../../../components/ui/NavBarTabs';
import { translate } from '../../../helpers/l10n';

export default function Nav() {
  return (
    <nav className="account-nav">
      <NavBarTabs>
        <li>
          <NavLink end={true} to="/account">
            {translate('my_account.profile')}
          </NavLink>
        </li>
        <li>
          <NavLink to="/account/security">{translate('my_account.security')}</NavLink>
        </li>
        <li>
          <NavLink to="/account/notifications">{translate('my_account.notifications')}</NavLink>
        </li>
        <li>
          <NavLink to="/account/projects">{translate('my_account.projects')}</NavLink>
        </li>
      </NavBarTabs>
    </nav>
  );
}
