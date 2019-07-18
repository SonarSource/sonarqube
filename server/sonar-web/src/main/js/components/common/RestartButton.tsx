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
import * as classNames from 'classnames';
import * as React from 'react';
import { Button } from 'sonar-ui-common/components/controls/buttons';
import ConfirmButton from 'sonar-ui-common/components/controls/ConfirmButton';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { restart } from '../../api/system';

interface Props {
  className?: string;
  fetchSystemStatus: () => void;
  systemStatus: T.SysStatus;
}

export default class RestartButton extends React.PureComponent<Props> {
  handleConfirm = () => {
    return restart().then(this.props.fetchSystemStatus);
  };

  render() {
    const { className, systemStatus } = this.props;
    return (
      <ConfirmButton
        confirmButtonText={translate('restart')}
        modalBody={
          <>
            <p className="spacer-top spacer-bottom">
              {translate('system.are_you_sure_to_restart')}
            </p>
            <p>{translate('system.forcing_shutdown_not_recommended')}</p>
          </>
        }
        modalHeader={translate('system.restart_server')}
        onConfirm={this.handleConfirm}>
        {({ onClick }) => (
          <Button
            className={classNames('button-red', className)}
            disabled={systemStatus !== 'UP'}
            onClick={onClick}>
            {systemStatus === 'RESTARTING'
              ? translate('system.restart_in_progress')
              : translate('system.restart_server')}
          </Button>
        )}
      </ConfirmButton>
    );
  }
}
