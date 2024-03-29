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
import { LabelValueSelectOption } from 'design-system';
import { debounce } from 'lodash';
import * as React from 'react';
import { Options } from 'react-select';
import { searchGroups, searchUsers } from '../../../api/quality-gates';
import { Group, SearchPermissionsParameters, isUser } from '../../../types/quality-gates';
import { QualityGate } from '../../../types/types';
import { UserBase } from '../../../types/users';
import QualityGatePermissionsAddModalRenderer from './QualityGatePermissionsAddModalRenderer';

type Option = UserBase | Group;
export type OptionWithValue = Option & { value: string };

interface Props {
  onClose: () => void;
  onSubmit: (selection: UserBase | Group) => void;
  qualityGate: QualityGate;
  submitting: boolean;
}

interface State {
  selection?: UserBase | Group;
}

const DEBOUNCE_DELAY = 250;

export default class QualityGatePermissionsAddModal extends React.Component<Props, State> {
  state: State = {};

  constructor(props: Props) {
    super(props);
    this.handleSearch = debounce(this.handleSearch, DEBOUNCE_DELAY);
  }

  handleSearch = (
    q: string,
    resolve: (options: Options<LabelValueSelectOption<UserBase | Group>>) => void,
  ) => {
    const { qualityGate } = this.props;

    const queryParams: SearchPermissionsParameters = {
      gateName: qualityGate.name,
      q,
      selected: 'deselected',
    };

    Promise.all([searchUsers(queryParams), searchGroups(queryParams)])
      .then(([usersResponse, groupsResponse]) =>
        [...usersResponse.users, ...groupsResponse.groups].map((o) => ({
          value: o,
          label: isUser(o) ? `${o.name} ${o.login}` : o.name,
        })),
      )
      .then(resolve)
      .catch(() => resolve([]));
  };

  handleSelection = ({ value }: LabelValueSelectOption<UserBase | Group>) => {
    this.setState({ selection: value });
  };

  handleSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { selection } = this.state;
    if (selection) {
      this.props.onSubmit(selection);
    }
  };

  render() {
    const { submitting } = this.props;
    const { selection } = this.state;

    return (
      <QualityGatePermissionsAddModalRenderer
        onClose={this.props.onClose}
        onSelection={this.handleSelection}
        onSubmit={this.handleSubmit}
        handleSearch={this.handleSearch}
        selection={selection}
        submitting={submitting}
      />
    );
  }
}
