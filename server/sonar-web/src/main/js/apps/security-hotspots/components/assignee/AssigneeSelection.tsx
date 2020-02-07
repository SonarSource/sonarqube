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
import { searchUsers } from '../../../../api/users';
import { isUserActive } from '../../../../helpers/users';
import AssigneeSelectionRenderer from './AssigneeSelectionRenderer';

interface Props {
  allowCurrentUserSelection: boolean;
  loggedInUser: T.LoggedInUser;
  onSelect: (user?: T.UserActive) => void;
}

interface State {
  highlighted?: T.UserActive;
  loading: boolean;
  open: boolean;
  query?: string;
  suggestedUsers: T.UserActive[];
}

export default class AssigneeSelection extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);

    this.state = {
      loading: false,
      open: props.allowCurrentUserSelection,
      suggestedUsers: props.allowCurrentUserSelection ? [props.loggedInUser] : []
    };

    this.handleSearch = debounce(this.handleSearch, 250);
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleSearch = (query: string) => {
    if (this.mounted) {
      if (query.length < 2) {
        this.handleNoSearch(query);
      } else {
        this.handleActualSearch(query);
      }
    }
  };

  handleNoSearch = (query: string) => {
    const { allowCurrentUserSelection, loggedInUser } = this.props;

    this.setState({
      loading: false,
      open: allowCurrentUserSelection,
      query,
      suggestedUsers: allowCurrentUserSelection ? [loggedInUser] : []
    });
  };

  handleActualSearch = (query: string) => {
    this.setState({ loading: true, query });
    searchUsers({ q: query })
      .then(result => {
        if (this.mounted) {
          this.setState({
            loading: false,
            query,
            open: true,
            suggestedUsers: result.users.filter(isUserActive) as T.UserActive[]
          });
        }
      })
      .catch(() => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      });
  };

  handleKeyDown = (event: React.KeyboardEvent) => {
    switch (event.keyCode) {
      case KeyCodes.Enter:
        event.preventDefault();
        this.selectHighlighted();
        break;
      case KeyCodes.UpArrow:
        event.preventDefault();
        this.highlightPrevious();
        break;
      case KeyCodes.DownArrow:
        event.preventDefault();
        this.highlightNext();
        break;
    }
  };

  getCurrentIndex = () => {
    const { highlighted, suggestedUsers } = this.state;

    return highlighted
      ? suggestedUsers.findIndex(suggestion => suggestion.login === highlighted.login)
      : -1;
  };

  highlightIndex = (index: number) => {
    const { suggestedUsers } = this.state;

    if (suggestedUsers.length > 0) {
      if (index < 0) {
        index = suggestedUsers.length - 1;
      } else if (index >= suggestedUsers.length) {
        index = 0;
      }

      this.setState({ highlighted: suggestedUsers[index] });
    }
  };

  highlightPrevious = () => {
    this.highlightIndex(this.getCurrentIndex() - 1);
  };

  highlightNext = () => {
    this.highlightIndex(this.getCurrentIndex() + 1);
  };

  selectHighlighted = () => {
    const { highlighted } = this.state;

    if (highlighted !== undefined) {
      this.props.onSelect(highlighted);
    }
  };

  render() {
    const { highlighted, loading, open, query, suggestedUsers } = this.state;

    return (
      <AssigneeSelectionRenderer
        highlighted={highlighted}
        loading={loading}
        onKeyDown={this.handleKeyDown}
        onSearch={this.handleSearch}
        onSelect={this.props.onSelect}
        open={open}
        query={query}
        suggestedUsers={suggestedUsers}
      />
    );
  }
}
