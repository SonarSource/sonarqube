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
import { EditButton } from '../../../../components/controls/buttons';
import EscKeydownHandler from '../../../../components/controls/EscKeydownHandler';
import OutsideClickHandler from '../../../../components/controls/OutsideClickHandler';
import DeferredSpinner from '../../../../components/ui/DeferredSpinner';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { LoggedInUser, UserActive, UserBase } from '../../../../types/users';
import AssigneeSelection from './AssigneeSelection';

export interface AssigneeRendererProps {
  canEdit: boolean;
  editing: boolean;
  loading: boolean;

  assignee?: UserBase;
  loggedInUser?: LoggedInUser;

  onAssign: (user: UserActive) => void;
  onEnterEditionMode: () => void;
  onExitEditionMode: () => void;
}

export default function AssigneeRenderer(props: AssigneeRendererProps) {
  const { assignee, canEdit, loggedInUser, editing, loading } = props;

  return (
    <DeferredSpinner loading={loading}>
      {!editing && (
        <div className="display-flex-center">
          <strong className="nowrap" data-testid="assignee-name">
            {assignee &&
              (assignee.active
                ? assignee.name ?? assignee.login
                : translateWithParameters('user.x_deleted', assignee.name ?? assignee.login))}
            {!assignee && translate('unassigned')}
          </strong>
          {loggedInUser && canEdit && (
            <EditButton
              aria-label={translate('hotspots.assignee.change_user')}
              className="spacer-left"
              onClick={props.onEnterEditionMode}
            />
          )}
        </div>
      )}

      {loggedInUser && editing && (
        <EscKeydownHandler onKeydown={props.onExitEditionMode}>
          <OutsideClickHandler onClickOutside={props.onExitEditionMode}>
            <AssigneeSelection
              allowCurrentUserSelection={loggedInUser.login !== assignee?.login}
              loggedInUser={loggedInUser}
              onSelect={props.onAssign}
            />
          </OutsideClickHandler>
        </EscKeydownHandler>
      )}
    </DeferredSpinner>
  );
}
