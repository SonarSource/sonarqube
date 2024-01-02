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
import { searchUsers } from '../../../../api/users';
import { KeyboardKeys } from '../../../../helpers/keycodes';
import { translate } from '../../../../helpers/l10n';
import { isUserActive, LoggedInUser, UserActive } from '../../../../types/users';
import AssigneeSelectionRenderer from './AssigneeSelectionRenderer';

interface Props {
  allowCurrentUserSelection: boolean;
  loggedInUser: LoggedInUser;
  onSelect: (user: UserActive) => void;
}

interface State {
  highlighted?: UserActive;
  loading: boolean;
  query?: string;
  suggestedUsers: UserActive[];
}

const UNASSIGNED: UserActive = { login: '', name: translate('unassigned') };

export default class AssigneeSelection extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);

    this.state = {
      loading: false,
      suggestedUsers: props.allowCurrentUserSelection
        ? [props.loggedInUser, UNASSIGNED]
        : [UNASSIGNED],
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
      query,
      suggestedUsers: allowCurrentUserSelection ? [loggedInUser, UNASSIGNED] : [UNASSIGNED],
    });
  };

  handleActualSearch = (query: string) => {
    this.setState({ loading: true, query });
    searchUsers({ q: query })
      .then((result) => {
        if (this.mounted) {
          this.setState({
            loading: false,
            query,
            suggestedUsers: (result.users.filter(isUserActive) as UserActive[]).concat(UNASSIGNED),
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
    switch (event.nativeEvent.key) {
      case KeyboardKeys.Enter:
        event.preventDefault();
        this.selectHighlighted();
        break;
      case KeyboardKeys.UpArrow:
        event.preventDefault();
        event.nativeEvent.stopImmediatePropagation();
        this.highlightPrevious();
        break;
      case KeyboardKeys.DownArrow:
        event.preventDefault();
        event.nativeEvent.stopImmediatePropagation();
        this.highlightNext();
        break;
    }
  };

  getCurrentIndex = () => {
    const { highlighted, suggestedUsers } = this.state;

    return highlighted
      ? suggestedUsers.findIndex((suggestion) => suggestion.login === highlighted.login)
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
    const { highlighted, loading, query, suggestedUsers } = this.state;

    return (
      <AssigneeSelectionRenderer
        highlighted={highlighted}
        loading={loading}
        onKeyDown={this.handleKeyDown}
        onSearch={this.handleSearch}
        onSelect={this.props.onSelect}
        query={query}
        suggestedUsers={suggestedUsers}
      />
    );
  }
}
