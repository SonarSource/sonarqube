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

import { ButtonSecondary, FlagMessage } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { cancelPendingPlugins } from '../../../../api/plugins';
import InstanceMessage from '../../../../components/common/InstanceMessage';
import RestartButton from '../../../../components/common/RestartButton';
import { translate } from '../../../../helpers/l10n';
import { PendingPluginResult } from '../../../../types/plugins';
import { SysStatus } from '../../../../types/types';

interface Props {
  fetchSystemStatus: () => void;
  pending: PendingPluginResult;
  refreshPending: () => void;
  systemStatus: SysStatus;
}

export default class PendingPluginsActionNotif extends React.PureComponent<Props> {
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
      <FlagMessage className="sw-w-full" variant="info">
        <div className="sw-flex sw-items-center">
          <span className="sw-mr-1">
            <InstanceMessage message={translate('marketplace.instance_needs_to_be_restarted_to')} />
          </span>
          {[
            { length: installing.length, msg: 'marketplace.install_x_plugins' },
            { length: updating.length, msg: 'marketplace.update_x_plugins' },
            { length: removing.length, msg: 'marketplace.uninstall_x_plugins' },
          ]
            .filter(({ length }) => length > 0)
            .map(({ length, msg }, idx) => (
              <span key={msg}>
                {idx > 0 && '; '}
                <FormattedMessage
                  defaultMessage={translate(msg)}
                  id={msg}
                  values={{ nb: <strong>{length}</strong> }}
                />
              </span>
            ))}

          <RestartButton
            className="sw-ml-2"
            fetchSystemStatus={this.props.fetchSystemStatus}
            systemStatus={this.props.systemStatus}
          />
          <ButtonSecondary className="sw-ml-2" onClick={this.handleRevert}>
            {translate('marketplace.revert')}
          </ButtonSecondary>
        </div>
      </FlagMessage>
    );
  }
}
