/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { FormattedMessage } from 'react-intl';
import RestartForm from '../../components/common/RestartForm';
import { cancelPendingPlugins, PluginPending } from '../../api/plugins';
import { translate } from '../../helpers/l10n';

interface Props {
  pending: {
    installing: PluginPending[];
    updating: PluginPending[];
    removing: PluginPending[];
  };
  refreshPending: () => void;
}

interface State {
  openRestart: boolean;
}

export default class PendingActions extends React.PureComponent<Props, State> {
  state: State = { openRestart: false };

  handleOpenRestart = () => this.setState({ openRestart: true });
  hanleCloseRestart = () => this.setState({ openRestart: false });

  handleRevert = () => {
    cancelPendingPlugins().then(this.props.refreshPending, () => {});
  };

  render() {
    const { installing, updating, removing } = this.props.pending;
    const hasPendingActions = installing.length || updating.length || removing.length;
    if (!hasPendingActions) {
      return null;
    }

    return (
      <div className="js-pending alert alert-warning">
        <div className="display-inline-block">
          <p>{translate('marketplace.sonarqube_needs_to_be_restarted_to')}</p>
          <ul className="list-styled spacer-top">
            {installing.length > 0 && (
              <li>
                <FormattedMessage
                  defaultMessage={translate('marketplace.install_x_plugins')}
                  id="marketplace.install_x_plugins"
                  values={{ nb: <strong>{installing.length}</strong> }}
                />
              </li>
            )}
            {updating.length > 0 && (
              <li>
                <FormattedMessage
                  defaultMessage={translate('marketplace.update_x_plugins')}
                  id="marketplace.update_x_plugins"
                  values={{ nb: <strong>{updating.length}</strong> }}
                />
              </li>
            )}
            {removing.length > 0 && (
              <li>
                <FormattedMessage
                  defaultMessage={translate('marketplace.uninstall_x_plugins')}
                  id="marketplace.uninstall_x_plugins"
                  values={{ nb: <strong>{removing.length}</strong> }}
                />
              </li>
            )}
          </ul>
        </div>
        <div className="pull-right">
          <button className="js-restart little-spacer-right" onClick={this.handleOpenRestart}>
            {translate('marketplace.restart')}
          </button>
          <button className="js-cancel-all button-red" onClick={this.handleRevert}>
            {translate('marketplace.revert')}
          </button>
        </div>
        {this.state.openRestart && <RestartForm onClose={this.hanleCloseRestart} />}
      </div>
    );
  }
}
