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
import { debounce } from 'lodash';
import * as React from 'react';
import { components, OptionProps, SingleValueProps } from 'react-select';
import { BasicSelectOption, SearchSelect } from '../../../components/controls/Select';
import Avatar from '../../../components/ui/Avatar';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Issue } from '../../../types/types';
import { CurrentUser, isLoggedIn, isUserActive } from '../../../types/users';
import { searchAssignees } from '../utils';

const DEBOUNCE_DELAY = 250;
// exported for test
export const MIN_QUERY_LENGTH = 2;

export interface AssigneeOption extends BasicSelectOption {
  avatar?: string;
  email?: string;
  label: string;
  value: string;
}

export interface AssigneeSelectProps {
  currentUser: CurrentUser;
  issues: Issue[];
  onAssigneeSelect: (assignee: AssigneeOption) => void;
  inputId: string;
}

export default class AssigneeSelect extends React.Component<AssigneeSelectProps> {
  constructor(props: AssigneeSelectProps) {
    super(props);

    this.handleAssigneeSearch = debounce(this.handleAssigneeSearch, DEBOUNCE_DELAY);
  }

  getDefaultAssignee = () => {
    const { currentUser, issues } = this.props;
    const options = [];

    if (isLoggedIn(currentUser)) {
      const canBeAssignedToMe =
        issues.filter((issue) => issue.assignee !== currentUser.login).length > 0;
      if (canBeAssignedToMe) {
        options.push({
          avatar: currentUser.avatar,
          label: currentUser.name,
          value: currentUser.login,
        });
      }
    }

    const canBeUnassigned = issues.filter((issue) => issue.assignee).length > 0;
    if (canBeUnassigned) {
      options.push({ label: translate('unassigned'), value: '' });
    }

    return options;
  };

  handleAssigneeSearch = (query: string, resolve: (options: AssigneeOption[]) => void) => {
    if (query.length < MIN_QUERY_LENGTH) {
      resolve([]);
      return;
    }

    searchAssignees(query)
      .then(({ results }) =>
        results.map((r) => {
          const userInfo = r.name || r.login;

          return {
            avatar: r.avatar,
            label: isUserActive(r) ? userInfo : translateWithParameters('user.x_deleted', userInfo),
            value: r.login,
          };
        })
      )
      .then(resolve)
      .catch(() => resolve([]));
  };

  renderAssignee = (option: AssigneeOption) => {
    return (
      <div className="display-flex-center">
        {option.avatar !== undefined && (
          <Avatar className="spacer-right" hash={option.avatar} name={option.label} size={16} />
        )}
        {option.label}
      </div>
    );
  };

  renderAssigneeOption = (props: OptionProps<AssigneeOption, false>) => (
    <components.Option {...props}>{this.renderAssignee(props.data)}</components.Option>
  );

  renderSingleAssignee = (props: SingleValueProps<AssigneeOption>) => (
    <components.SingleValue {...props}>{this.renderAssignee(props.data)}</components.SingleValue>
  );

  render() {
    const { inputId } = this.props;
    return (
      <SearchSelect
        className="input-super-large"
        inputId={inputId}
        components={{
          Option: this.renderAssigneeOption,
          SingleValue: this.renderSingleAssignee,
        }}
        isClearable={true}
        defaultOptions={this.getDefaultAssignee()}
        loadOptions={this.handleAssigneeSearch}
        onChange={this.props.onAssigneeSelect}
        noOptionsMessage={({ inputValue }) =>
          inputValue.length < MIN_QUERY_LENGTH
            ? translateWithParameters('select2.tooShort', MIN_QUERY_LENGTH)
            : translate('select2.noMatches')
        }
      />
    );
  }
}
