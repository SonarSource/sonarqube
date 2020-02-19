/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { EditButton } from 'sonar-ui-common/components/controls/buttons';
import EscKeydownHandler from 'sonar-ui-common/components/controls/EscKeydownHandler';
import OutsideClickHandler from 'sonar-ui-common/components/controls/OutsideClickHandler';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import AssigneeSelection from './AssigneeSelection';

export interface AssigneeRendererProps {
  canEdit: boolean;
  editing: boolean;
  loading: boolean;

  assignee?: T.UserBase;
  loggedInUser?: T.LoggedInUser;

  onAssign: (user?: T.UserActive) => void;
  onEnterEditionMode: () => void;
  onExitEditionMode: () => void;
}

export default function AssigneeRenderer(props: AssigneeRendererProps) {
  const { assignee, canEdit, loggedInUser, editing, loading } = props;

  return (
    <DeferredSpinner loading={loading}>
      {!editing && (
        <div className="display-flex-center">
          <strong className="nowrap">
            {assignee &&
              (assignee.active
                ? assignee.name ?? assignee.login
                : translateWithParameters('user.x_deleted', assignee.name ?? assignee.login))}
            {!assignee && translate('unassigned')}
          </strong>
          {loggedInUser && canEdit && (
            <EditButton className="spacer-left" onClick={props.onEnterEditionMode} />
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
