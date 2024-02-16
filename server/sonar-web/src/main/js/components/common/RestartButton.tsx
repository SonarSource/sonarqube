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

import { DangerButtonSecondary } from 'design-system';
import * as React from 'react';
import { restart } from '../../api/system';
import ConfirmButton from '../../components/controls/ConfirmButton';
import { translate } from '../../helpers/l10n';
import { SysStatus } from '../../types/types';

interface Props {
  className?: string;
  fetchSystemStatus: () => void;
  systemStatus: SysStatus;
}

export default class RestartButton extends React.PureComponent<Props> {
  handleConfirm = () => restart().then(this.props.fetchSystemStatus);

  render() {
    const { className, systemStatus } = this.props;
    return (
      <ConfirmButton
        confirmButtonText={translate('restart')}
        modalBody={
          <>
            <p className="sw-my-2">{translate('system.are_you_sure_to_restart')}</p>
            <p className="sw-mb-2">{translate('system.forcing_shutdown_not_recommended')}</p>
            <p>{translate('system.restart_does_not_reload_sonar_properties')}</p>
          </>
        }
        modalHeader={translate('system.restart_server')}
        onConfirm={this.handleConfirm}
      >
        {({ onClick }) => (
          <DangerButtonSecondary
            className={className}
            disabled={systemStatus !== 'UP'}
            onClick={onClick}
          >
            {systemStatus === 'RESTARTING'
              ? translate('system.restart_in_progress')
              : translate('system.restart_server')}
          </DangerButtonSecondary>
        )}
      </ConfirmButton>
    );
  }
}
