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
import CheckIcon from 'sonar-ui-common/components/icons/CheckIcon';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { colors } from '../../../app/theme';

const TRIAL_DURATION_DAYS = 14;

export default function UpgradeOrganizationAdvantages() {
  return (
    <ul className="note">
      <Advantage>{translate('billing.upgrade_box.unlimited_private_projects')}</Advantage>
      <Advantage>{translate('billing.upgrade_box.strict_control_private_data')}</Advantage>
      <Advantage>{translate('billing.upgrade_box.cancel_anytime')}</Advantage>
      <Advantage>
        <strong>
          {translateWithParameters('billing.upgrade_box.free_trial_x', TRIAL_DURATION_DAYS)}
        </strong>
      </Advantage>
    </ul>
  );
}

export function Advantage({ children }: { children: React.ReactNode }) {
  return (
    <li className="display-flex-center little-spacer-bottom">
      <CheckIcon className="spacer-right" fill={colors.lightGreen} />
      {children}
    </li>
  );
}
