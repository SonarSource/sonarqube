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
import { Spinner } from '@sonarsource/echoes-react';
import { Card, ClipboardButton, FlagMessage, Title } from 'design-system';
import * as React from 'react';
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import AppVersionStatus from '../../../components/shared/AppVersionStatus';
import { toShortISO8601String } from '../../../helpers/dates';
import { translate } from '../../../helpers/l10n';
import { AppState } from '../../../types/appstate';
import PageActions from './PageActions';

export interface Props {
  isCluster: boolean;
  loading: boolean;
  logLevel: string;
  onLogLevelChange: () => void;
  appState: AppState;
  serverId?: string;
  version?: string;
}

function PageHeader(props: Readonly<Props>) {
  const { isCluster, loading, logLevel, serverId, version, appState } = props;
  return (
    <header className="sw-mt-8">
      <div className="sw-flex sw-items-start sw-justify-between">
        <Title>{translate('system_info.page')}</Title>

        <div className="sw-flex sw-items-center">
          <Spinner className="sw-mr-4 sw-mt-1" isLoading={loading} />

          <PageActions
            canDownloadLogs={!isCluster}
            cluster={isCluster}
            logLevel={logLevel}
            onLogLevelChange={props.onLogLevelChange}
            serverId={serverId}
          />
        </div>
      </div>

      {serverId && version && (
        <Card className="sw-max-w-1/2 sw-mb-8">
          {!appState.productionDatabase && (
            <FlagMessage className="sw-mb-2" variant="warning">
              {translate('system.not_production_database_warning')}
            </FlagMessage>
          )}
          <div className="sw-flex sw-items-center sw-justify-between">
            <div>
              <div className="sw-flex sw-items-center">
                <strong className="sw-w-32">{translate('system.server_id')}</strong>
                <span className="sw-code">{serverId}</span>
              </div>
              <div className="sw-flex sw-items-center">
                <strong className="sw-w-32">{translate('system.version')}</strong>
                <span>
                  <AppVersionStatus />
                </span>
              </div>
            </div>
            <ClipboardButton
              className="sw-ml-4"
              copyValue={`SonarQube ID information
Server ID: ${serverId}
Version: ${version}
Date: ${toShortISO8601String(Date.now())}
`}
            >
              <span className="sw-ml-1 sw-whitespace-nowrap">
                {translate('system.copy_id_info')}
              </span>
            </ClipboardButton>
          </div>
        </Card>
      )}
    </header>
  );
}

export default withAppStateContext(PageHeader);
