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
import { groupBy, isEmpty, mapValues } from 'lodash';
import * as React from 'react';
import { getSystemUpgrades } from '../../../api/system';
import { Alert, AlertVariant } from '../../../components/ui/Alert';
import DismissableAlert from '../../../components/ui/DismissableAlert';
import SystemUpgradeButton from '../../../components/upgrade/SystemUpgradeButton';
import { sortUpgrades, UpdateUseCase } from '../../../components/upgrade/utils';
import { translate } from '../../../helpers/l10n';
import { hasGlobalPermission } from '../../../helpers/users';
import { AppState } from '../../../types/appstate';
import { Permissions } from '../../../types/permissions';
import { SystemUpgrade } from '../../../types/system';
import { Dict } from '../../../types/types';
import { CurrentUser, isLoggedIn } from '../../../types/users';
import withAppStateContext from '../app-state/withAppStateContext';
import withCurrentUserContext from '../current-user/withCurrentUserContext';
import './UpdateNotification.css';

const MONTH_BEFOR_PREVIOUS_LTS_NOTIFICATION = 6;

type GroupedSystemUpdate = {
  [x: string]: Dict<SystemUpgrade[]>;
};

const MAP_VARIANT: Dict<AlertVariant> = {
  [UpdateUseCase.NewMinorVersion]: 'info',
  [UpdateUseCase.NewPatch]: 'warning',
  [UpdateUseCase.PreLTS]: 'warning',
  [UpdateUseCase.PreviousLTS]: 'error',
};

interface Props {
  dismissable: boolean;
  appState: AppState;
  currentUser: CurrentUser;
}

interface State {
  dismissKey: string;
  useCase: UpdateUseCase;
  latestLTS: string;
  systemUpgrades: SystemUpgrade[];
  canSeeNotification: boolean;
}

export class UpdateNotification extends React.PureComponent<Props, State> {
  mounted = false;
  versionParser = /^(\d+)\.(\d+)(\.(\d+))?/;

  constructor(props: Props) {
    super(props);
    this.state = {
      dismissKey: '',
      systemUpgrades: [],
      latestLTS: '',
      canSeeNotification: false,
      useCase: UpdateUseCase.NewMinorVersion,
    };
  }

  componentDidMount() {
    this.mounted = true;

    this.fetchSystemUpgradeInformation();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  isPreLTSUpdate(parsedVersion: number[], latestLTS: string) {
    const [currentMajor, currentMinor] = parsedVersion;
    const [ltsMajor, ltsMinor] = latestLTS.split('.').map(Number);
    return currentMajor < ltsMajor || (currentMajor === ltsMajor && currentMinor < ltsMinor);
  }

  isPreviousLTSUpdate(
    parsedVersion: number[],
    latestLTS: string,
    systemUpgrades: GroupedSystemUpdate
  ) {
    const [ltsMajor, ltsMinor] = latestLTS.split('.').map(Number);
    let ltsOlderThan6Month = false;
    const beforeLts = this.isPreLTSUpdate(parsedVersion, latestLTS);
    if (beforeLts) {
      const allLTS = sortUpgrades(systemUpgrades[ltsMajor][ltsMinor]);
      const ltsReleaseDate = new Date(allLTS[allLTS.length - 1]?.releaseDate || '');
      if (isNaN(ltsReleaseDate.getTime())) {
        // We can not parse the LTS date.
        // It is unlikly that this could happen but consider LTS to be old.
        return true;
      }
      ltsOlderThan6Month =
        ltsReleaseDate.setMonth(ltsReleaseDate.getMonth() + MONTH_BEFOR_PREVIOUS_LTS_NOTIFICATION) -
          Date.now() <
        0;
    }
    return ltsOlderThan6Month && beforeLts;
  }

  isMinorUpdate(parsedVersion: number[], systemUpgrades: GroupedSystemUpdate) {
    const [currentMajor, currentMinor] = parsedVersion;
    const allMinor = systemUpgrades[currentMajor];
    return Object.keys(allMinor)
      .map(Number)
      .some((minor) => minor > currentMinor);
  }

  isPatchUpdate(parsedVersion: number[], systemUpgrades: GroupedSystemUpdate) {
    const [currentMajor, currentMinor, currentPatch] = parsedVersion;
    const allMinor = systemUpgrades[currentMajor];
    const allPatch = sortUpgrades(allMinor[currentMinor] || []);

    if (!isEmpty(allPatch)) {
      const [, , latestPatch] = allPatch[0].version.split('.').map(Number);
      const effectiveCurrentPatch = isNaN(currentPatch) ? 0 : currentPatch;
      const effectiveLatestPatch = isNaN(latestPatch) ? 0 : latestPatch;
      return effectiveCurrentPatch < effectiveLatestPatch;
    }
    return false;
  }

  async fetchSystemUpgradeInformation() {
    if (
      !isLoggedIn(this.props.currentUser) ||
      !hasGlobalPermission(this.props.currentUser, Permissions.Admin)
    ) {
      this.noPromptToShow();
      return;
    }

    const regExpParsedVersion = this.versionParser.exec(this.props.appState.version);
    if (regExpParsedVersion === null) {
      this.noPromptToShow();
      return;
    }
    regExpParsedVersion.shift();
    const parsedVersion = regExpParsedVersion.map(Number).map((n) => (isNaN(n) ? 0 : n));

    const { upgrades, latestLTS } = await getSystemUpgrades();

    if (isEmpty(upgrades)) {
      // No new upgrades
      this.noPromptToShow();
      return;
    }
    const systemUpgrades = mapValues(
      groupBy(upgrades, (upgrade) => {
        const [major] = upgrade.version.split('.');
        return major;
      }),
      (upgrades) =>
        groupBy(upgrades, (upgrade) => {
          const [, minor] = upgrade.version.split('.');
          return minor;
        })
    );

    let useCase = UpdateUseCase.NewMinorVersion;

    if (this.isPreviousLTSUpdate(parsedVersion, latestLTS, systemUpgrades)) {
      useCase = UpdateUseCase.PreviousLTS;
    } else if (this.isPreLTSUpdate(parsedVersion, latestLTS)) {
      useCase = UpdateUseCase.PreLTS;
    } else if (this.isPatchUpdate(parsedVersion, systemUpgrades)) {
      useCase = UpdateUseCase.NewPatch;
    } else if (this.isMinorUpdate(parsedVersion, systemUpgrades)) {
      useCase = UpdateUseCase.NewMinorVersion;
    }

    const latest = [...upgrades].sort(
      (upgrade1, upgrade2) =>
        new Date(upgrade2.releaseDate || '').getTime() -
        new Date(upgrade1.releaseDate || '').getTime()
    )[0];

    const dismissKey = useCase + latest.version;

    if (this.mounted) {
      this.setState({
        latestLTS,
        useCase,
        dismissKey,
        systemUpgrades: upgrades,
        canSeeNotification: true,
      });
    }
  }

  noPromptToShow() {
    if (this.mounted) {
      this.setState({ canSeeNotification: false });
    }
  }

  render() {
    const { dismissable } = this.props;
    const { latestLTS, systemUpgrades, canSeeNotification, useCase, dismissKey } = this.state;
    if (!canSeeNotification) {
      return null;
    }
    return dismissable ? (
      <DismissableAlert
        alertKey={dismissKey}
        variant={MAP_VARIANT[useCase]}
        className={`promote-update-notification it__upgrade-prompt-${useCase}`}
      >
        {translate('admin_notification.update', useCase)}
        <SystemUpgradeButton
          systemUpgrades={systemUpgrades}
          updateUseCase={useCase}
          latestLTS={latestLTS}
        />
      </DismissableAlert>
    ) : (
      <Alert variant={MAP_VARIANT[useCase]} className={`it__upgrade-prompt-${useCase}`}>
        {translate('admin_notification.update', useCase)}
        <SystemUpgradeButton
          systemUpgrades={systemUpgrades}
          updateUseCase={useCase}
          latestLTS={latestLTS}
        />
      </Alert>
    );
  }
}

export default withCurrentUserContext(withAppStateContext(UpdateNotification));
