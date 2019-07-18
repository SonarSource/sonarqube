/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import Select from 'sonar-ui-common/components/controls/Select';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import Avatar from '../../../components/ui/Avatar';

interface Option {
  login: string;
  name: string;
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

  componentDidUpdate(prevProps: Props) {
    if (this.props.excludedUsers !== prevProps.excludedUsers) {
      this.handleSearch(this.state.search);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  filterSearchResult = ({ users }: { users: Option[] }) =>
    users.filter(user => !this.props.excludedUsers.includes(user.login)).slice(0, LIST_SIZE);

  filterOptions = (options: Option[]) => {
    return options; // We don't filter anything, this is done by the WS
  };

  handleSearch = (search: string) => {
    this.props
      .searchUsers(search, Math.min(this.props.excludedUsers.length + LIST_SIZE, 500))
      .then(this.filterSearchResult)
      .then(
        searchResult => {
          if (this.mounted) {
            this.setState({ isLoading: false, searchResult });
          }
        },
        () => {
          if (this.mounted) {
            this.setState({ isLoading: false, searchResult: [] });
          }
        }
      );
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
        autoFocus={this.props.autoFocus}
        className="Select-big"
        clearable={false}
        filterOptions={this.filterOptions}
        isLoading={this.state.isLoading}
        labelKey="name"
        noResultsText={noResult}
        onChange={this.props.handleValueChange}
        onInputChange={this.handleInputChange}
        optionComponent={UsersSelectSearchOption}
        options={this.state.searchResult}
        placeholder=""
        searchable={true}
        value={this.props.selectedUser}
        valueComponent={UsersSelectSearchValue}
        valueKey="login"
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
        role="listitem"
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
      {value && value.login && (
        <div className="Select-value-label">
          <Avatar hash={value.avatar} name={value.name} size={AVATAR_SIZE} />
          <strong className="spacer-left">{children}</strong>
          <span className="note little-spacer-left">{value.login}</span>
        </div>
      )}
    </div>
  );
}
