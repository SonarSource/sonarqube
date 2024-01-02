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
import EmbedDocsPopupHelper from '../../../../components/embed-docs-modal/EmbedDocsPopupHelper';
import NavBar from '../../../../components/ui/NavBar';
import { CurrentUser } from '../../../../types/users';
import { rawSizes } from '../../../theme';
import withCurrentUserContext from '../../current-user/withCurrentUserContext';
import Search from '../../search/Search';
import './GlobalNav.css';
import GlobalNavBranding from './GlobalNavBranding';
import GlobalNavMenu from './GlobalNavMenu';
import GlobalNavUser from './GlobalNavUser';

export interface GlobalNavProps {
  currentUser: CurrentUser;
  location: { pathname: string };
}

export function GlobalNav(props: GlobalNavProps) {
  const { currentUser, location } = props;
  return (
    <NavBar className="navbar-global" height={rawSizes.globalNavHeightRaw} id="global-navigation">
      <GlobalNavBranding />

      <GlobalNavMenu currentUser={currentUser} location={location} />

      <div className="global-navbar-menu global-navbar-menu-right">
        <EmbedDocsPopupHelper />
        <Search />
        <GlobalNavUser currentUser={currentUser} />
      </div>
    </NavBar>
  );
}

export default withCurrentUserContext(GlobalNav);
