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
import { LabelValueSelectOption, SearchSelectDropdown } from 'design-system';
import * as React from 'react';
import { Options, SingleValue } from 'react-select';
import { CurrentUserContext } from '../../../app/components/current-user/CurrentUserContext';
import Avatar from '../../../components/ui/Avatar';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Issue } from '../../../types/types';
import { RestUser, UserActive, isLoggedIn, isUserActive } from '../../../types/users';
import { searchAssignees } from '../utils';

// exported for test
export const MIN_QUERY_LENGTH = 2;

const UNASSIGNED = { value: '', label: translate('unassigned') };

export interface AssigneeSelectProps {
  assignee?: SingleValue<LabelValueSelectOption>;
  className?: string;
  issues: Issue[];
  onAssigneeSelect: (assignee: SingleValue<LabelValueSelectOption>) => void;
  inputId: string;
}

function userToOption(user: RestUser | UserActive) {
  const userInfo = user.name || user.login;
  return {
    value: user.login,
    label: isUserActive(user) ? userInfo : translateWithParameters('user.x_deleted', userInfo),
    Icon: <Avatar hash={user.avatar} name={user.name} size="xs" className="sw-my-1" />,
  };
}

export default function AssigneeSelect(props: AssigneeSelectProps) {
  const { assignee, className, issues, inputId } = props;

  const { currentUser } = React.useContext(CurrentUserContext);

  const allowCurrentUserSelection =
    isLoggedIn(currentUser) && issues.some((issue) => currentUser.login !== issue.assignee);

  const defaultOptions = allowCurrentUserSelection
    ? [UNASSIGNED, userToOption(currentUser)]
    : [UNASSIGNED];

  const controlLabel = assignee ? (
    <>
      {assignee.Icon} {assignee.label}
    </>
  ) : (
    translate('select_verb')
  );

  const handleAssigneeSearch = React.useCallback(
    (query: string, resolve: (options: Options<LabelValueSelectOption<string>>) => void) => {
      if (query.length < MIN_QUERY_LENGTH) {
        resolve([]);
        return;
      }

      searchAssignees(query)
        .then(({ results }) => results.map(userToOption))
        .then(resolve)
        .catch(() => resolve([]));
    },
    [],
  );

  return (
    <SearchSelectDropdown
      aria-label={translate('search.search_for_users')}
      className={className}
      size="full"
      controlSize="full"
      inputId={inputId}
      defaultOptions={defaultOptions}
      loadOptions={handleAssigneeSearch}
      onChange={props.onAssigneeSelect}
      noOptionsMessage={({ inputValue }) =>
        inputValue.length < MIN_QUERY_LENGTH
          ? translateWithParameters('select2.tooShort', MIN_QUERY_LENGTH)
          : translate('select2.noMatches')
      }
      placeholder={translate('search.search_for_users')}
      controlLabel={controlLabel}
      controlAriaLabel={translate('issue_bulk_change.assignee.change')}
    />
  );
}
