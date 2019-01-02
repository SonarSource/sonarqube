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
import SystemUpgradeForm from './SystemUpgradeForm';
import { sortUpgrades, groupUpgrades } from '../../utils';
import { getSystemUpgrades, SystemUpgrade } from '../../../../api/system';
import { Button } from '../../../../components/ui/buttons';
import { translate } from '../../../../helpers/l10n';
import { Alert } from '../../../../components/ui/Alert';

interface State {
  systemUpgrades: SystemUpgrade[][];
  openSystemUpgradeForm: boolean;
}

export default class SystemUpgradeNotif extends React.PureComponent<{}, State> {
  mounted = false;
  state: State = { openSystemUpgradeForm: false, systemUpgrades: [] };

  componentDidMount() {
    this.mounted = true;
    this.fetchSystemUpgrade();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchSystemUpgrade = () =>
    getSystemUpgrades().then(
      ({ upgrades }) => {
        if (this.mounted) {
          this.setState({ systemUpgrades: groupUpgrades(sortUpgrades(upgrades)) });
        }
      },
      () => {}
    );

  handleOpenSystemUpgradeForm = () => {
    this.setState({ openSystemUpgradeForm: true });
  };

  handleCloseSystemUpgradeForm = () => {
    this.setState({ openSystemUpgradeForm: false });
  };

  render() {
    const { systemUpgrades } = this.state;

    if (systemUpgrades.length <= 0) {
      return null;
    }

    return (
      <div className="page-notifs">
        <Alert variant="info">
          {translate('system.new_version_available')}
          <Button className="spacer-left" onClick={this.handleOpenSystemUpgradeForm}>
            {translate('learn_more')}
          </Button>
        </Alert>
        {this.state.openSystemUpgradeForm && (
          <SystemUpgradeForm
            onClose={this.handleCloseSystemUpgradeForm}
            systemUpgrades={systemUpgrades}
          />
        )}
      </div>
    );
  }
}
