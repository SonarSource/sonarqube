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
import { ButtonSecondary } from 'design-system';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import { SystemUpgrade } from '../../types/system';
import SystemUpgradeForm from './SystemUpgradeForm';
import { groupUpgrades, sortUpgrades, UpdateUseCase } from './utils';

interface Props {
  latestLTS: string;
  systemUpgrades: SystemUpgrade[];
  updateUseCase?: UpdateUseCase;
}

export default function SystemUpgradeButton(props: Readonly<Props>) {
  const { latestLTS, systemUpgrades, updateUseCase } = props;

  const [isSystemUpgradeFormOpen, setSystemUpgradeFormOpen] = React.useState(false);

  const openSystemUpgradeForm = React.useCallback(() => {
    setSystemUpgradeFormOpen(true);
  }, [setSystemUpgradeFormOpen]);

  const closeSystemUpgradeForm = React.useCallback(() => {
    setSystemUpgradeFormOpen(false);
  }, [setSystemUpgradeFormOpen]);

  return (
    <>
      <ButtonSecondary className="sw-ml-2" onClick={openSystemUpgradeForm}>
        {translate('learn_more')}
      </ButtonSecondary>
      {isSystemUpgradeFormOpen && (
        <SystemUpgradeForm
          onClose={closeSystemUpgradeForm}
          systemUpgrades={groupUpgrades(sortUpgrades(systemUpgrades))}
          latestLTS={latestLTS}
          updateUseCase={updateUseCase}
        />
      )}
    </>
  );
}
