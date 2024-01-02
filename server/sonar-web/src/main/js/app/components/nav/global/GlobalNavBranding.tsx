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
import Link from '../../../../components/common/Link';
import { translate } from '../../../../helpers/l10n';
import { getBaseUrl } from '../../../../helpers/system';
import { AppState } from '../../../../types/appstate';
import { GlobalSettingKeys } from '../../../../types/settings';
import withAppStateContext from '../../app-state/withAppStateContext';

export interface GlobalNavBrandingProps {
  appState: AppState;
}

export function GlobalNavBranding({ appState: { settings } }: GlobalNavBrandingProps) {
  const customLogoUrl = settings[GlobalSettingKeys.LogoUrl];
  const customLogoWidth = settings[GlobalSettingKeys.LogoWidth];

  const title = translate('layout.nav.home_logo_alt');
  const url = customLogoUrl || `${getBaseUrl()}/images/logo.svg?v=6.6`;
  const width = customLogoUrl ? customLogoWidth || 100 : 83;

  return (
    <Link className="navbar-brand" to="/">
      <img alt={title} height={30} src={url} title={title} width={width} />
    </Link>
  );
}

export default withAppStateContext(GlobalNavBranding);
