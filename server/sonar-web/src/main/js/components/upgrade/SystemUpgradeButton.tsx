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

import { Button, Link } from '@sonarsource/echoes-react';
import React from 'react';
import { translate } from '../../helpers/l10n';
import { SystemUpgrade } from '../../types/system';
import { SystemUpgradeForm } from './SystemUpgradeForm';
import { groupUpgrades, sortUpgrades, UpdateUseCase } from './utils';

interface Props {
  latestLTA?: string;
  systemUpgrades: SystemUpgrade[];
  updateUseCase: UpdateUseCase;
}

export function SystemUpgradeButton(props: Readonly<Props>) {
  const { latestLTA, systemUpgrades, updateUseCase } = props;

  const [isSystemUpgradeFormOpen, setIsSystemUpgradeFormOpen] = React.useState(false);

  const openSystemUpgradeForm = React.useCallback(() => {
    setIsSystemUpgradeFormOpen(true);
  }, [setIsSystemUpgradeFormOpen]);

  const closeSystemUpgradeForm = React.useCallback(() => {
    setIsSystemUpgradeFormOpen(false);
  }, [setIsSystemUpgradeFormOpen]);

  if (systemUpgrades.length === 0) {
    return (
      <Link
        className="sw-ml-2"
        to="https://www.sonarsource.com/products/sonarqube/downloads/?referrer=sonarqube"
        shouldOpenInNewTab
      >
        {translate('learn_more')}
      </Link>
    );
  }

  return (
    <>
      <Button className="sw-ml-2" onClick={openSystemUpgradeForm}>
        {translate('learn_more')}
      </Button>

      {isSystemUpgradeFormOpen && (
        <SystemUpgradeForm
          onClose={closeSystemUpgradeForm}
          systemUpgrades={groupUpgrades(sortUpgrades(systemUpgrades))}
          latestLTA={latestLTA}
          updateUseCase={updateUseCase}
        />
      )}
    </>
  );
}
