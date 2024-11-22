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
import DocLink from '../../components/common/DocLink';
import InstanceMessage from '../../components/common/InstanceMessage';
import Link from '../../components/common/Link';
import AppVersionStatus from '../../components/shared/AppVersionStatus';
import { Alert } from '../../components/ui/Alert';
import { getEdition } from '../../helpers/editions';
import { translate } from '../../helpers/l10n';
import { AppState } from '../../types/appstate';
import GlobalFooterBranding from './GlobalFooterBranding';
import withAppStateContext from './app-state/withAppStateContext';

export interface GlobalFooterProps {
  hideLoggedInInfo?: boolean;
  appState?: AppState;
}

export function GlobalFooter({ hideLoggedInInfo, appState }: GlobalFooterProps) {
  const currentEdition = appState?.edition && getEdition(appState.edition);

  return (
    <div className="page-footer page-container" id="footer">
      {appState?.productionDatabase === false && (
        <Alert display="inline" id="evaluation_warning" variant="warning">
          <p className="big">{translate('footer.production_database_warning')}</p>
          <p>
            <InstanceMessage message={translate('footer.production_database_explanation')} />
          </p>
        </Alert>
      )}

      <GlobalFooterBranding />

      <ul className="page-footer-menu">
        {!hideLoggedInInfo && currentEdition && (
          <li className="page-footer-menu-item">{currentEdition.name}</li>
        )}
        {!hideLoggedInInfo && appState?.version && (
          <li className="page-footer-menu-item">
            <AppVersionStatus />
          </li>
        )}
        <li className="page-footer-menu-item">
          <Link
            to="https://www.gnu.org/licenses/lgpl-3.0.txt"
            rel="noopener noreferrer"
            target="_blank"
          >
            {translate('footer.license')}
          </Link>
        </li>
        <li className="page-footer-menu-item">
          <Link
            to="https://community.sonarsource.com/c/help/sq"
            rel="noopener noreferrer"
            target="_blank"
          >
            {translate('footer.community')}
          </Link>
        </li>
        <li className="page-footer-menu-item">
          <DocLink to="/">{translate('footer.documentation')}</DocLink>
        </li>
        <li className="page-footer-menu-item">
          <DocLink to="/instance-administration/plugin-version-matrix/">
            {translate('footer.plugins')}
          </DocLink>
        </li>
        {!hideLoggedInInfo && (
          <li className="page-footer-menu-item">
            <Link to="/web_api">{translate('footer.web_api')}</Link>
          </li>
        )}
      </ul>
    </div>
  );
}

export default withAppStateContext(GlobalFooter);
