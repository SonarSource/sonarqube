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
import { CurrentUser } from '../../../../types/users';
import withCurrentUserContext from '../../current-user/withCurrentUserContext';
import GlobalSearch from '../../global-search/GlobalSearch';
import GlobalNavMenu from './GlobalNavMenu';
import { GlobalNavUser } from './GlobalNavUser';
import MainSonarQubeBar from './MainSonarQubeBar';

export interface GlobalNavProps {
  currentUser: CurrentUser;
  location: { pathname: string };
}

export function GlobalNav(props: GlobalNavProps) {
  const { currentUser, location } = props;
  return (
    <MainSonarQubeBar>
      <div className="sw-flex" id="global-navigation">
        <div className="it__global-navbar-menu sw-flex sw-justify-start sw-items-center sw-flex-1">
          <GlobalNavMenu currentUser={currentUser} location={location} />
          <div className="sw-px-8 sw-flex-1">
            <GlobalSearch />
          </div>
        </div>

        <div className="sw-flex sw-items-center sw-ml-2">
          <EmbedDocsPopupHelper />
          <div className="sw-ml-4">
            <GlobalNavUser />
          </div>
        </div>
      </div>
    </MainSonarQubeBar>
  );
}

export default withCurrentUserContext(GlobalNav);
