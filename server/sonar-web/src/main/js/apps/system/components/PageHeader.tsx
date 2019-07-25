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
import { ClipboardButton } from 'sonar-ui-common/components/controls/clipboard';
import { toShortNotSoISOString } from 'sonar-ui-common/helpers/dates';
import { translate } from 'sonar-ui-common/helpers/l10n';
import PageActions from './PageActions';

export interface Props {
  isCluster: boolean;
  loading: boolean;
  logLevel: string;
  onLogLevelChange: () => void;
  serverId?: string;
  showActions: boolean;
  version?: string;
}

export default function PageHeader(props: Props) {
  const { isCluster, loading, logLevel, serverId, showActions, version } = props;
  return (
    <header className="page-header">
      <h1 className="page-title">{translate('system_info.page')}</h1>
      {showActions && (
        <PageActions
          canDownloadLogs={!isCluster}
          canRestart={!isCluster}
          cluster={isCluster}
          logLevel={logLevel}
          onLogLevelChange={props.onLogLevelChange}
          serverId={serverId}
        />
      )}
      {loading && (
        <div className="page-actions">
          <i className="spinner" />
        </div>
      )}
      {serverId && version && (
        <div className="system-info-copy-paste-id-info boxed-group display-flex-center">
          <div className="flex-1">
            <table className="width-100">
              <tbody>
                <tr>
                  <th>
                    <strong>{translate('system.server_id')}</strong>
                  </th>
                  <td>{serverId}</td>
                </tr>
                <tr>
                  <th>
                    <strong>{translate('system.version')}</strong>
                  </th>
                  <td>{version}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <ClipboardButton
            className="flex-0"
            copyValue={`SonarQube ID information
Server ID: ${serverId}
Version: ${version}
Date: ${toShortNotSoISOString(Date.now())}
`}>
            {translate('system.copy_id_info')}
          </ClipboardButton>
        </div>
      )}
    </header>
  );
}
