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
import { debounce } from 'lodash';
import * as React from 'react';
import { KeyCodes } from 'sonar-ui-common/helpers/keycodes';
import { searchUsers } from '../../../api/users';
import { isUserActive } from '../../../helpers/users';
import HotspotAssigneeSelectRenderer from './HotspotAssigneeSelectRenderer';

interface Props {
  onSelect: (user?: T.UserActive) => void;
}

interface State {
  highlighted?: T.UserActive;
  loading: boolean;
  open: boolean;
  query?: string;
  suggestedUsers?: T.UserActive[];
}

export default class HotspotAssigneeSelect extends React.PureComponent<Props, State> {
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {
      loading: false,
      open: false
    };
    this.handleSearch = debounce(this.handleSearch, 250);
  }

  getCurrentIndex = () => {
    const { highlighted, suggestedUsers } = this.state;
    return highlighted && suggestedUsers
      ? suggestedUsers.findIndex(suggestion => suggestion.login === highlighted.login)
      : -1;
  };

  handleSearch = (query: string) => {
    if (query.length < 2) {
      this.setState({ open: false, query });
      this.props.onSelect(undefined);
      return Promise.resolve([]);
    }

    this.setState({ loading: true, query });
    return searchUsers({ q: query })
      .then(this.handleSearchResult, () => {})
      .catch(() => this.setState({ loading: false }));
  };

  handleSearchResult = ({ users }: { users: T.UserBase[] }) => {
    const activeUsers = users.filter(isUserActive);
    this.setState(({ highlighted }) => {
      if (activeUsers.length === 0) {
        highlighted = undefined;
      } else {
        const findHighlited = activeUsers.find(u => highlighted && u.login === highlighted.login);
        highlighted = findHighlited || activeUsers[0];
      }

      return {
        highlighted,
        loading: false,
        open: true,
        suggestedUsers: activeUsers
      };
    });
  };

  handleKeyDown = (event: React.KeyboardEvent) => {
    switch (event.keyCode) {
      case KeyCodes.Enter:
        event.preventDefault();
        this.handleSelectHighlighted();
        break;
      case KeyCodes.UpArrow:
        event.preventDefault();
        this.handleHighlightPrevious();
        break;
      case KeyCodes.DownArrow:
        event.preventDefault();
        this.handleHighlightNext();
        break;
    }
  };

  highlightIndex = (index: number) => {
    const { suggestedUsers } = this.state;
    if (suggestedUsers && suggestedUsers.length > 0) {
      if (index < 0) {
        index = suggestedUsers.length - 1;
      } else if (index >= suggestedUsers.length) {
        index = 0;
      }
      this.setState({
        highlighted: suggestedUsers[index]
      });
    }
  };

  handleHighlightPrevious = () => {
    this.highlightIndex(this.getCurrentIndex() - 1);
  };

  handleHighlightNext = () => {
    this.highlightIndex(this.getCurrentIndex() + 1);
  };

  handleSelectHighlighted = () => {
    const { highlighted } = this.state;
    if (highlighted !== undefined) {
      this.handleSelect(highlighted);
    }
  };

  handleSelect = (selectedUser: T.UserActive) => {
    this.setState({
      open: false,
      query: selectedUser.name
    });
    this.props.onSelect(selectedUser);
  };

  render() {
    const { highlighted, loading, open, query, suggestedUsers } = this.state;
    return (
      <HotspotAssigneeSelectRenderer
        highlighted={highlighted}
        loading={loading}
        onKeyDown={this.handleKeyDown}
        onSearch={this.handleSearch}
        onSelect={this.handleSelect}
        open={open}
        query={query}
        suggestedUsers={suggestedUsers}
      />
    );
  }
}
