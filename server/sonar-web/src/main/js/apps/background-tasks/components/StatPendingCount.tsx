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
import { DestructiveIcon, TrashIcon } from 'design-system';
import * as React from 'react';
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import Tooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';
import { AppState } from '../../../types/appstate';

export interface Props {
  appState: AppState;
  onCancelAllPending: () => void;
  pendingCount?: number;
}

function StatPendingCount({ appState, onCancelAllPending, pendingCount }: Readonly<Props>) {
  if (pendingCount === undefined) {
    return null;
  }

  return (
    <div className="sw-flex sw-items-center">
      <span className="sw-body-md-highlight sw-mr-1">{pendingCount}</span>
      {translate('background_tasks.pending')}
      {appState.canAdmin && pendingCount > 0 && (
        <ConfirmButton
          cancelButtonText={translate('close')}
          confirmButtonText={translate('background_tasks.cancel_all_tasks.submit')}
          isDestructive
          modalBody={translate('background_tasks.cancel_all_tasks.text')}
          modalHeader={translate('background_tasks.cancel_all_tasks')}
          onConfirm={onCancelAllPending}
        >
          {({ onClick }) => (
            <Tooltip content={translate('background_tasks.cancel_all_tasks')}>
              <DestructiveIcon
                aria-label={translate('background_tasks.cancel_all_tasks')}
                className="sw-ml-1"
                Icon={TrashIcon}
                onClick={onClick}
              />
            </Tooltip>
          )}
        </ConfirmButton>
      )}
    </div>
  );
}

export default withAppStateContext(StatPendingCount);
