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
import { Dropdown, ItemNavLink, MainMenuItem, PopupPlacement, PopupZLevel } from 'design-system';
import * as React from 'react';
import { translate } from '../../../../helpers/l10n';
import { AppState } from '../../../../types/appstate';
import { Extension } from '../../../../types/types';
import withAppStateContext from '../../app-state/withAppStateContext';

const renderGlobalPageLink = ({ key, name }: Extension) => {
  return (
    <ItemNavLink key={key} to={`/extension/${key}`}>
      {name}
    </ItemNavLink>
  );
};

function GlobalNavMore({ appState: { globalPages = [] } }: { appState: AppState }) {
  const withoutPortfolios = globalPages.filter((page) => page.key !== 'governance/portfolios');

  if (withoutPortfolios.length === 0) {
    return null;
  }

  return (
    <Dropdown
      id="moreMenuDropdown"
      overlay={<ul>{withoutPortfolios.map(renderGlobalPageLink)}</ul>}
      placement={PopupPlacement.BottomLeft}
      zLevel={PopupZLevel.Global}
    >
      {({ onToggleClick, open }) => (
        <ul>
          <MainMenuItem>
            <a
              aria-expanded={open}
              aria-haspopup="menu"
              href="#"
              id="global-navigation-more"
              onClick={onToggleClick}
              role="button"
            >
              {translate('more')}
            </a>
          </MainMenuItem>
        </ul>
      )}
    </Dropdown>
  );
}

export default withAppStateContext(GlobalNavMore);
