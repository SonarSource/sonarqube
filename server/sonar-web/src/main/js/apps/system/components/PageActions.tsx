/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { Button, EditButton } from '../../../components/controls/buttons';
import Dropdown from '../../../components/controls/Dropdown';
import DropdownIcon from '../../../components/icons/DropdownIcon';
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
      <div className="page-actions">
        <span>
          <span className="text-middle">
            {translate('system.logs_level')}
            {':'}
            <strong className="little-spacer-left">{this.props.logLevel}</strong>
          </span>
          <EditButton
            className="spacer-left button-small"
            id="edit-logs-level-button"
            onClick={this.handleLogsLevelOpen}
            aria-label={translate('system.logs_level.change')}
          />
        </span>
        {this.props.canDownloadLogs && (
          <Dropdown
            className="display-inline-block spacer-left"
            overlay={
              <ul className="menu">
                <li>
                  <a
                    download="sonarqube_app.log"
                    href={logsUrl + '?name=app'}
                    id="logs-link"
                    rel="noopener noreferrer"
                    target="_blank"
                  >
                    Main Process
                  </a>
                </li>
                <li>
                  <a
                    download="sonarqube_ce.log"
                    href={logsUrl + '?name=ce'}
                    id="ce-logs-link"
                    rel="noopener noreferrer"
                    target="_blank"
                  >
                    Compute Engine
                  </a>
                </li>
                <li>
                  <a
                    download="sonarqube_es.log"
                    href={logsUrl + '?name=es'}
                    id="es-logs-link"
                    rel="noopener noreferrer"
                    target="_blank"
                  >
                    Search Engine
                  </a>
                </li>
                <li>
                  <a
                    download="sonarqube_web.log"
                    href={logsUrl + '?name=web'}
                    id="web-logs-link"
                    rel="noopener noreferrer"
                    target="_blank"
                  >
                    Web Server
                  </a>
                </li>
                <li>
                  <a
                    download="sonarqube_access.log"
                    href={logsUrl + '?name=access'}
                    id="access-logs-link"
                    rel="noopener noreferrer"
                    target="_blank"
                  >
                    Access Logs
                  </a>
                </li>
                <li>
                  <a
                    download="sonarqube_deprecation.log"
                    href={logsUrl + '?name=deprecation'}
                    rel="noopener noreferrer"
                    target="_blank"
                  >
                    Deprecation Logs
                  </a>
                </li>
              </ul>
            }
          >
            <Button>
              {translate('system.download_logs')}
              <DropdownIcon className="little-spacer-left" />
            </Button>
          </Dropdown>
        )}
        <a
          className="button spacer-left"
          download={`sonarqube-system-info-${getFileNameSuffix(this.props.serverId)}.json`}
          href={infoUrl}
          id="download-link"
          onClick={this.removeElementFocus}
          rel="noopener noreferrer"
          target="_blank"
        >
          {translate('system.download_system_info')}
        </a>
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
