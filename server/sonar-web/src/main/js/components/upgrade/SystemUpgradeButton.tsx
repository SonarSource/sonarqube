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
import { translate } from '../../helpers/l10n';
import { SystemUpgrade } from '../../types/system';
import { Button } from '../controls/buttons';
import SystemUpgradeForm from './SystemUpgradeForm';
import { groupUpgrades, sortUpgrades, UpdateUseCase } from './utils';

interface Props {
  latestLTS: string;
  systemUpgrades: SystemUpgrade[];
  updateUseCase?: UpdateUseCase;
}

interface State {
  openSystemUpgradeForm: boolean;
}

export default class SystemUpgradeButton extends React.PureComponent<Props, State> {
  state: State = { openSystemUpgradeForm: false };

  handleOpenSystemUpgradeForm = () => {
    this.setState({ openSystemUpgradeForm: true });
  };

  handleCloseSystemUpgradeForm = () => {
    this.setState({ openSystemUpgradeForm: false });
  };

  render() {
    const { latestLTS, systemUpgrades, updateUseCase } = this.props;
    const { openSystemUpgradeForm } = this.state;

    if (systemUpgrades.length === 0) {
      return null;
    }

    return (
      <>
        <Button className="spacer-left" onClick={this.handleOpenSystemUpgradeForm}>
          {translate('learn_more')}
        </Button>
        {openSystemUpgradeForm && (
          <SystemUpgradeForm
            onClose={this.handleCloseSystemUpgradeForm}
            systemUpgrades={groupUpgrades(sortUpgrades(systemUpgrades))}
            latestLTS={latestLTS}
            updateUseCase={updateUseCase}
          />
        )}
      </>
    );
  }
}
