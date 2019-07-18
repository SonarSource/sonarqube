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
import { connect } from 'react-redux';
import { ClearButton } from 'sonar-ui-common/components/controls/buttons';
import ConfirmButton from 'sonar-ui-common/components/controls/ConfirmButton';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { colors } from '../../../app/theme';
import { getAppState, Store } from '../../../store/rootReducer';

export interface Props {
  isSystemAdmin?: boolean;
  onCancelAllPending: () => void;
  pendingCount?: number;
}

export function StatPendingCount({ isSystemAdmin, onCancelAllPending, pendingCount }: Props) {
  if (pendingCount === undefined) {
    return null;
  }

  return (
    <span>
      <span className="emphasised-measure">{pendingCount}</span>
      <span className="little-spacer-left display-inline-flex-center">
        {translate('background_tasks.pending')}
        {isSystemAdmin && pendingCount > 0 && (
          <ConfirmButton
            cancelButtonText={translate('close')}
            confirmButtonText={translate('background_tasks.cancel_all_tasks.submit')}
            isDestructive={true}
            modalBody={translate('background_tasks.cancel_all_tasks.text')}
            modalHeader={translate('background_tasks.cancel_all_tasks')}
            onConfirm={onCancelAllPending}>
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

const mapStateToProps = (state: Store) => ({
  isSystemAdmin: getAppState(state).canAdmin
});

export default connect(mapStateToProps)(StatPendingCount);
