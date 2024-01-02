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
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import { colors } from '../../../app/theme';
import { ClearButton } from '../../../components/controls/buttons';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import Tooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';
import { AppState } from '../../../types/appstate';

export interface Props {
  appState: AppState;
  onCancelAllPending: () => void;
  pendingCount?: number;
}

export function StatPendingCount({ appState, onCancelAllPending, pendingCount }: Props) {
  if (pendingCount === undefined) {
    return null;
  }

  return (
    <span>
      <span className="emphasised-measure">{pendingCount}</span>
      <span className="little-spacer-left display-inline-flex-center">
        {translate('background_tasks.pending')}
        {appState.canAdmin && pendingCount > 0 && (
          <ConfirmButton
            cancelButtonText={translate('close')}
            confirmButtonText={translate('background_tasks.cancel_all_tasks.submit')}
            isDestructive={true}
            modalBody={translate('background_tasks.cancel_all_tasks.text')}
            modalHeader={translate('background_tasks.cancel_all_tasks')}
            onConfirm={onCancelAllPending}
          >
            {({ onClick }) => (
              <Tooltip overlay={translate('background_tasks.cancel_all_tasks')}>
                <ClearButton className="little-spacer-left" color={colors.red} onClick={onClick} />
              </Tooltip>
            )}
          </ConfirmButton>
        )}
      </span>
    </span>
  );
}

export default withAppStateContext(StatPendingCount);
