/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { Link } from 'react-router';
import * as theme from '../../app/theme';
import ContextNavBar from '../../components/nav/ContextNavBar';
import NavBarTabs from '../../components/nav/NavBarTabs';
import { translate } from '../../helpers/l10n';

interface Props {
  children: JSX.Element;
}

export default function Explore(props: Props) {
  return (
    <div id="explore">
      <ContextNavBar height={theme.contextNavHeightRaw} id="explore-navigation">
        <header className="navbar-context-header">
          <h1>{translate('explore')}</h1>
        </header>

        <NavBarTabs>
          <li>
            <Link activeClassName="active" to="/explore/projects">
              {translate('projects.page')}
            </Link>
          </li>
          <li>
            <Link
              activeClassName="active"
              to={{ pathname: '/explore/issues', query: { resolved: 'false' } }}>
              {translate('issues.page')}
            </Link>
          </li>
        </NavBarTabs>
      </ContextNavBar>

      {props.children}
    </div>
  );
}
