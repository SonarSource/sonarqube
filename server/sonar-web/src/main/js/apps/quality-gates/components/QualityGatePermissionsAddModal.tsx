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
import { searchGroups, searchUsers } from '../../../api/quality-gates';
import { Group, SearchPermissionsParameters, isUser } from '../../../types/quality-gates';
import { QualityGate } from '../../../types/types';
import { UserBase } from '../../../types/users';
import { QGPermissionOption } from '../utils';
import QualityGatePermissionsAddModalRenderer from './QualityGatePermissionsAddModalRenderer';

interface Props {
  onClose: () => void;
  onSubmit: (selection: UserBase | Group) => void;
  qualityGate: QualityGate;
  submitting: boolean;
}

interface State {
  loading: boolean;
  options: Array<QGPermissionOption>;
  selection?: UserBase | Group;
}

const DEBOUNCE_DELAY = 250;

export default class QualityGatePermissionsAddModal extends React.Component<Props, State> {
  state: State = {
    loading: false,
    options: [],
  };

  constructor(props: Props) {
    super(props);
    this.handleSearch = debounce(this.handleSearch, DEBOUNCE_DELAY);
  }

  handleSearch = (q: string) => {
    const { qualityGate } = this.props;

    const queryParams: SearchPermissionsParameters = {
      gateName: qualityGate.name,
      q,
      selected: 'deselected',
    };

    this.setState({ loading: true });

    Promise.all([searchUsers(queryParams), searchGroups(queryParams)])
      .then(([usersResponse, groupsResponse]) =>
        [...usersResponse.users, ...groupsResponse.groups].map(
          (o) =>
            ({
              ...o,
              value: isUser(o) ? o.login : o.name,
              label: isUser(o) ? (o.name ?? o.login) : o.name,
            }) as QGPermissionOption,
        ),
      )
      .then((options) => {
        this.setState({ loading: false, options });
      })
      .catch(() => {
        this.setState({ loading: false, options: [] });
      });
  };

  handleSelection = (selectionKey?: string) => {
    this.setState(({ options }) => {
      const selectedOption = selectionKey
        ? options.find((o) => (isUser(o) ? o.login : o.name) === selectionKey)
        : undefined;
      return { selection: selectedOption };
    });
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
    const { loading, options, selection } = this.state;

    return (
      <QualityGatePermissionsAddModalRenderer
        loading={loading}
        onClose={this.props.onClose}
        onSelection={this.handleSelection}
        onSubmit={this.handleSubmit}
        options={options}
        handleSearch={this.handleSearch}
        selection={selection}
        submitting={submitting}
      />
    );
  }
}
