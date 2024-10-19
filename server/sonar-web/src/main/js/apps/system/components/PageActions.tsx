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
import {
  ButtonPrimary,
  ChevronDownIcon,
  DownloadButton,
  Dropdown,
  InteractiveIcon,
  ItemDownload,
  PencilIcon,
} from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { getFileNameSuffix } from '../utils';
import ChangeLogLevelForm from './ChangeLogLevelForm';

interface Props {
  canDownloadLogs: boolean;
  cluster: boolean;
  logLevel: string;
  onLogLevelChange: () => void;
  serverId?: string;
}

interface State {
  openLogsLevelForm: boolean;
}

export default class PageActions extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      openLogsLevelForm: false,
    };
  }

  handleLogsLevelOpen = () => {
    this.setState({ openLogsLevelForm: true });
  };

  handleLogsLevelChange = () => {
    this.props.onLogLevelChange();
    this.handleLogsLevelClose();
  };

  handleLogsLevelClose = () => {
    this.setState({ openLogsLevelForm: false });
  };

  removeElementFocus = (event: React.SyntheticEvent<HTMLElement>) => {
    event.currentTarget.blur();
  };

  render() {
    const infoUrl = getBaseUrl() + '/api/system/info';
    const logsUrl = getBaseUrl() + '/api/system/logs';
    return (
      <div className="sw-flex sw-items-center sw-gap-2">
        <div className="sw-flex sw-items-center">
          <span>
            {translate('system.logs_level')}
            {': '}
            <strong>{this.props.logLevel}</strong>
          </span>
          <InteractiveIcon
            className="sw-ml-1"
            Icon={PencilIcon}
            id="edit-logs-level-button"
            onClick={this.handleLogsLevelOpen}
            aria-label={translate('system.logs_level.change')}
          />
        </div>
        {this.props.canDownloadLogs && (
          <Dropdown
            id="system-logs-download"
            overlay={
              <>
                <ItemDownload download="sonarqube_app.log" href={logsUrl + '?name=app'}>
                  Main Process
                </ItemDownload>
                <ItemDownload download="sonarqube_ce.log" href={logsUrl + '?name=ce'}>
                  Compute Engine
                </ItemDownload>
                <ItemDownload download="sonarqube_es.log" href={logsUrl + '?name=es'}>
                  Search Engine
                </ItemDownload>

                <ItemDownload download="sonarqube_web.log" href={logsUrl + '?name=web'}>
                  Web Server
                </ItemDownload>

                <ItemDownload download="sonarqube_access.log" href={logsUrl + '?name=access'}>
                  Access Logs
                </ItemDownload>

                <ItemDownload
                  download="sonarqube_deprecation.log"
                  href={logsUrl + '?name=deprecation'}
                >
                  Deprecation Logs
                </ItemDownload>
              </>
            }
          >
            <ButtonPrimary>
              {translate('system.download_logs')}
              <ChevronDownIcon className="sw-ml-1" />
            </ButtonPrimary>
          </Dropdown>
        )}
        <DownloadButton
          download={`sonarqube-system-info-${getFileNameSuffix(this.props.serverId)}.json`}
          href={infoUrl}
          id="download-link"
          onClick={this.removeElementFocus}
          rel="noopener noreferrer"
          target="_blank"
        >
          {translate('system.download_system_info')}
        </DownloadButton>
        {this.state.openLogsLevelForm && (
          <ChangeLogLevelForm
            infoMsg={translate(
              this.props.cluster ? 'system.cluster_log_level.info' : 'system.log_level.info',
            )}
            logLevel={this.props.logLevel}
            onChange={this.handleLogsLevelChange}
            onClose={this.handleLogsLevelClose}
          />
        )}
      </div>
    );
  }
}
