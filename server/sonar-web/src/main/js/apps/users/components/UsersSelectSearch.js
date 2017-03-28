/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
//@flow
import React from 'react';
import Select from 'react-select';
import { debounce } from 'lodash';
import UsersSelectSearchOption from './UsersSelectSearchOption';
import UsersSelectSearchValue from './UsersSelectSearchValue';
import './UsersSelectSearch.css';

export type Option = {
  login: string,
  name: string,
  email?: string,
  avatar?: string,
  groupCount?: number
};

type Props = {
  selectedUser?: Option,
  excludedUsers: Array<string>,
  searchUsers: (string, number) => Promise<*>,
  handleValueChange: (Option) => void
};

type State = {
  searchResult: Array<Option>,
  isLoading: boolean,
  search: string
};

const LIST_SIZE = 10;

export default class UsersSelectSearch extends React.PureComponent {
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.handleSearch = debounce(this.handleSearch);
    this.state = { searchResult: [], isLoading: false, search: '' };
  }

  componentDidMount() {
    this.handleSearch(this.state.search);
  }

  componentWillReceiveProps(nextProps: Props) {
    if (this.props.excludedUsers !== nextProps.excludedUsers) {
      this.handleSearch(this.state.search);
    }
  }

  filterSearchResult = ({ users }: { users: Array<Option> }) =>
    users.filter(user => !this.props.excludedUsers.includes(user.login)).slice(0, LIST_SIZE);

  handleSearch = (search: string) => {
    this.setState({ isLoading: true, search });
    this.props.searchUsers(search, Math.min(this.props.excludedUsers.length + LIST_SIZE, 500))
      .then(this.filterSearchResult)
      .then(searchResult => {
        this.setState({ isLoading: false, searchResult });
      });
  };

  render() {
    return (
      <Select
        className="Select-big"
        options={this.state.searchResult}
        isLoading={this.state.isLoading}
        optionComponent={UsersSelectSearchOption}
        valueComponent={UsersSelectSearchValue}
        onChange={this.props.handleValueChange}
        onInputChange={this.handleSearch}
        value={this.props.selectedUser}
        placeholder=""
        labelKey="name"
        valueKey="login"
        clearable={false}
        searchable={true}
      />
    );
  }
}
