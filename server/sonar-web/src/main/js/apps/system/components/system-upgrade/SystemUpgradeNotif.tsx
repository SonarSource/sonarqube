/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { getSystemUpgrades } from '../../../../api/system';
import { Alert } from '../../../../components/ui/Alert';
import SystemUpgradeButton from '../../../../components/upgrade/SystemUpgradeButton';
import { translate } from '../../../../helpers/l10n';
import { SystemUpgrade } from '../../../../types/system';

interface State {
  systemUpgrades: SystemUpgrade[];
}

export default class SystemUpgradeNotif extends React.PureComponent<{}, State> {
  mounted = false;
  state: State = { systemUpgrades: [] };

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
          this.setState({ systemUpgrades: upgrades });
        }
      },
      () => {}
    );

  render() {
    const { systemUpgrades } = this.state;

    if (systemUpgrades.length <= 0) {
      return null;
    }

    return (
      <div className="page-notifs">
        <Alert variant="info">
          {translate('system.new_version_available')}
          <SystemUpgradeButton systemUpgrades={systemUpgrades} />
        </Alert>
      </div>
    );
  }
}
