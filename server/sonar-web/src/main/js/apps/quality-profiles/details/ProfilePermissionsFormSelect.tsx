/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { debounce, identity, omit } from 'lodash';
import * as React from 'react';
import { components, ControlProps, OptionProps, SingleValueProps } from 'react-select';
import Select from '../../../components/controls/Select';
import GroupIcon from '../../../components/icons/GroupIcon';
import Avatar from '../../../components/ui/Avatar';
import { translate } from '../../../helpers/l10n';
import { UserSelected } from '../../../types/types';
import { Group } from './ProfilePermissions';

type Option = UserSelected | Group;
type OptionWithValue = Option & { value: string };

interface Props {
  onChange: (option: OptionWithValue) => void;
  onSearch: (query: string) => Promise<Option[]>;
  selected?: Option;
}

interface State {
  loading: boolean;
  query: string;
  searchResults: Option[];
}

export default class ProfilePermissionsFormSelect extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.handleSearch = debounce(this.handleSearch, 250);
    this.state = { loading: false, query: '', searchResults: [] };
  }

  componentDidMount() {
    this.mounted = true;
    this.handleSearch(this.state.query);
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleSearch = (query: string) => {
    this.setState({ loading: true });
    this.props.onSearch(query).then(
      searchResults => {
        if (this.mounted) {
          this.setState({ loading: false, searchResults });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  handleInputChange = (newQuery: string) => {
    const { query } = this.state;
    if (query !== newQuery) {
      this.setState({ query: newQuery });
      this.handleSearch(newQuery);
    }
  };

  optionRenderer(props: OptionProps<OptionWithValue, false>) {
    const { data } = props;
    return (
      <components.Option {...props} className="Select-option">
        {customOptions(data)}
      </components.Option>
    );
  }

  singleValueRenderer = (props: SingleValueProps<OptionWithValue>) => (
    <components.SingleValue {...props} className="Select-value-label">
      {customOptions(props.data)}
    </components.SingleValue>
  );

  controlRenderer = (props: ControlProps<OptionWithValue, false>) => (
    <components.Control {...omit(props, ['children'])} className="abs-height-100 Select-control">
      {props.children}
    </components.Control>
  );

  render() {
    const noResultsText = translate('no_results');
    const { selected } = this.props;
    // create a uniq string both for users and groups
    const options = this.state.searchResults.map(r => ({ ...r, value: getStringValue(r) }));

    // when user input is empty the options shows only top 30 names
    // the below code add the selected user so that it appears too
    if (
      selected !== undefined &&
      options.find(o => o.value === getStringValue(selected)) === undefined
    ) {
      options.unshift({ ...selected, value: getStringValue(selected) });
    }

    return (
      <Select
        className="Select-big width-100"
        autoFocus={true}
        isClearable={false}
        id="change-profile-permission"
        inputId="change-profile-permission-input"
        onChange={this.props.onChange}
        onInputChange={this.handleInputChange}
        placeholder=""
        noOptionsMessage={() => noResultsText}
        isLoading={this.state.loading}
        options={options}
        isSearchable={true}
        filterOptions={identity}
        components={{
          Option: this.optionRenderer,
          SingleValue: this.singleValueRenderer,
          Control: this.controlRenderer
        }}
        value={options.filter(
          o => o.value === (this.props.selected && getStringValue(this.props.selected))
        )}
      />
    );
  }
}

function isUser(option: Option): option is UserSelected {
  return (option as UserSelected).login !== undefined;
}

function getStringValue(option: Option) {
  return isUser(option) ? `user:${option.login}` : `group:${option.name}`;
}

function customOptions(option: OptionWithValue) {
  return isUser(option) ? (
    <>
      <Avatar hash={option.avatar} name={option.name} size={16} />
      <strong className="spacer-left">{option.name}</strong>
      <span className="note little-spacer-left">{option.login}</span>
    </>
  ) : (
    <>
      <GroupIcon size={16} />
      <strong className="spacer-left">{option.name}</strong>
    </>
  );
}
