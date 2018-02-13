/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { debounce } from 'lodash';
import Avatar from '../../../components/ui/Avatar';
import Select from '../../../components/controls/Select';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Option {
  login: string;
  name: string;
  email?: string;
  avatar?: string;
}

interface Props {
  autoFocus?: boolean;
  excludedUsers: string[];
  handleValueChange: (option: Option) => void;
  searchUsers: (query: string, ps: number) => Promise<{ users: Option[] }>;
  selectedUser?: Option;
}

interface State {
  isLoading: boolean;
  search: string;
  searchResult: Option[];
}

const LIST_SIZE = 10;
const AVATAR_SIZE = 16;

export default class UsersSelectSearch extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.handleSearch = debounce(this.handleSearch, 250);
    this.state = { searchResult: [], isLoading: false, search: '' };
  }

  componentDidMount() {
    this.mounted = true;
    this.handleSearch(this.state.search);
  }

  componentWillReceiveProps(nextProps: Props) {
    if (this.props.excludedUsers !== nextProps.excludedUsers) {
      this.handleSearch(this.state.search);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  filterSearchResult = ({ users }: { users: Option[] }) =>
    users.filter(user => !this.props.excludedUsers.includes(user.login)).slice(0, LIST_SIZE);

  handleSearch = (search: string) => {
    this.props
      .searchUsers(search, Math.min(this.props.excludedUsers.length + LIST_SIZE, 500))
      .then(this.filterSearchResult)
      .then(searchResult => {
        if (this.mounted) {
          this.setState({ isLoading: false, searchResult });
        }
      });
  };

  handleInputChange = (search: string) => {
    if (search == null || search.length === 1) {
      this.setState({ search });
    } else {
      this.setState({ isLoading: true, search });
      this.handleSearch(search);
    }
  };

  render() {
    const noResult =
      this.state.search.length === 1
        ? translateWithParameters('select2.tooShort', 2)
        : translate('no_results');
    return (
      <Select
        autofocus={this.props.autoFocus}
        className="Select-big"
        options={this.state.searchResult}
        isLoading={this.state.isLoading}
        optionComponent={UsersSelectSearchOption}
        valueComponent={UsersSelectSearchValue}
        onChange={this.props.handleValueChange}
        onInputChange={this.handleInputChange}
        value={this.props.selectedUser}
        placeholder=""
        noResultsText={noResult}
        labelKey="name"
        valueKey="login"
        clearable={false}
        searchable={true}
      />
    );
  }
}

interface OptionProps {
  children?: React.ReactNode;
  className?: string;
  isFocused?: boolean;
  onFocus: (option: Option, evt: React.MouseEvent<HTMLDivElement>) => void;
  onSelect: (option: Option, evt: React.MouseEvent<HTMLDivElement>) => void;
  option: Option;
}

export class UsersSelectSearchOption extends React.PureComponent<OptionProps> {
  handleMouseDown = (evt: React.MouseEvent<HTMLDivElement>) => {
    evt.preventDefault();
    evt.stopPropagation();
    this.props.onSelect(this.props.option, evt);
  };

  handleMouseEnter = (evt: React.MouseEvent<HTMLDivElement>) => {
    this.props.onFocus(this.props.option, evt);
  };

  handleMouseMove = (evt: React.MouseEvent<HTMLDivElement>) => {
    if (this.props.isFocused) {
      return;
    }
    this.props.onFocus(this.props.option, evt);
  };

  render() {
    const { option } = this.props;
    return (
      <div
        className={this.props.className}
        onMouseDown={this.handleMouseDown}
        onMouseEnter={this.handleMouseEnter}
        onMouseMove={this.handleMouseMove}
        title={option.name}>
        <Avatar hash={option.avatar} name={option.name} size={AVATAR_SIZE} />
        <strong className="spacer-left">{this.props.children}</strong>
        <span className="note little-spacer-left">{option.login}</span>
      </div>
    );
  }
}

interface ValueProps {
  value?: Option;
  children?: React.ReactNode;
}

export function UsersSelectSearchValue({ children, value }: ValueProps) {
  return (
    <div className="Select-value" title={value ? value.name : ''}>
      {value &&
        value.login && (
          <div className="Select-value-label">
            <Avatar hash={value.avatar} name={value.name} size={AVATAR_SIZE} />
            <strong className="spacer-left">{children}</strong>
            <span className="note little-spacer-left">{value.login}</span>
          </div>
        )}
    </div>
  );
}
