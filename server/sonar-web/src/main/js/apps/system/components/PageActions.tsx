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
import { Button, EditButton } from 'sonar-ui-common/components/controls/buttons';
import Dropdown from 'sonar-ui-common/components/controls/Dropdown';
import DropdownIcon from 'sonar-ui-common/components/icons/DropdownIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import AdminContext from '../../../app/components/AdminContext';
import RestartButton from '../../../components/common/RestartButton';
import { getFileNameSuffix } from '../utils';
import ChangeLogLevelForm from './ChangeLogLevelForm';

interface Props {
  canDownloadLogs: boolean;
  canRestart: boolean;
  cluster: boolean;
  logLevel: string;
  onLogLevelChange: () => void;
  serverId?: string;
}

interface State {
  logLevel: string;
  openLogsLevelForm: boolean;
}

export default class PageActions extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      logLevel: props.logLevel,
      openLogsLevelForm: false
    };
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.logLevel !== this.state.logLevel) {
      this.setState({ logLevel: nextProps.logLevel });
    }
  }

  handleLogsLevelOpen = () => {
    this.setState({ openLogsLevelForm: true });
  };

  handleLogsLevelChange = (logLevel: string) => {
    this.setState({ logLevel });
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
            <strong className="little-spacer-left">{this.state.logLevel}</strong>
          </span>
          <EditButton
            className="spacer-left button-small"
            id="edit-logs-level-button"
            onClick={this.handleLogsLevelOpen}
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
                    href={logsUrl + '?process=app'}
                    id="logs-link"
                    rel="noopener noreferrer"
                    target="_blank">
                    Main Process
                  </a>
                </li>
                <li>
                  <a
                    download="sonarqube_ce.log"
                    href={logsUrl + '?process=ce'}
                    id="ce-logs-link"
                    rel="noopener noreferrer"
                    target="_blank">
                    Compute Engine
                  </a>
                </li>
                <li>
                  <a
                    download="sonarqube_es.log"
                    href={logsUrl + '?process=es'}
                    id="es-logs-link"
                    rel="noopener noreferrer"
                    target="_blank">
                    Search Engine
                  </a>
                </li>
                <li>
                  <a
                    download="sonarqube_web.log"
                    href={logsUrl + '?process=web'}
                    id="web-logs-link"
                    rel="noopener noreferrer"
                    target="_blank">
                    Web Server
                  </a>
                </li>
              </ul>
            }>
            <Button>
              {translate('system.download_logs')}
              <DropdownIcon className="little-spacer-left" />
            </Button>
          </Dropdown>
        )}
        <a
          className="button spacer-left"
          download={`sonarqube-support-info-${getFileNameSuffix(this.props.serverId)}.json`}
          href={infoUrl}
          id="download-link"
          onClick={this.removeElementFocus}
          rel="noopener noreferrer"
          target="_blank">
          {translate('system.download_system_info')}
        </a>
        {this.props.canRestart && (
          <AdminContext.Consumer>
            {({ fetchSystemStatus, systemStatus }) => (
              <RestartButton
                className="spacer-left"
                fetchSystemStatus={fetchSystemStatus}
                systemStatus={systemStatus}
              />
            )}
          </AdminContext.Consumer>
        )}
        {this.state.openLogsLevelForm && (
          <ChangeLogLevelForm
            infoMsg={translate(
              this.props.cluster ? 'system.cluster_log_level.info' : 'system.log_level.info'
            )}
            logLevel={this.state.logLevel}
            onChange={this.handleLogsLevelChange}
            onClose={this.handleLogsLevelClose}
          />
        )}
      </div>
    );
  }
}
