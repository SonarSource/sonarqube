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
import React from 'react';
import Select from 'react-select';
import difference from 'lodash/difference';
import { getFilterUrl } from './utils';
import { translate } from '../../../helpers/l10n';

export default class SearchableFilterFooter extends React.PureComponent {
  static propTypes = {
    property: React.PropTypes.string.isRequired,
    query: React.PropTypes.object.isRequired,
    getOptionLabel: React.PropTypes.func.isRequired,
    getOptions: React.PropTypes.func.isRequired,
    isAsync: React.PropTypes.bool,
    value: React.PropTypes.any,
    facet: React.PropTypes.object
  };

  handleLanguageChange = ({ value }) => {
    const urlOptions = (this.props.value || []).concat(value).join(',');
    const path = getFilterUrl(this.props, { [this.props.property]: urlOptions });
    this.props.router.push(path);
  };

  filterOptions = options => {
    const { facet } = this.props;
    let optionKeys = Array.isArray(options) ? options : Object.keys(options);
    if (facet) {
      optionKeys = difference(optionKeys, Object.keys(facet));
    }
    return optionKeys.map(key => ({ label: this.props.getOptionLabel(options, key), value: key }));
  };

  loadOptions = searchInput => {
    return this.props
        .getOptions(searchInput)
        .then(this.filterOptions)
        .then(options => ({ options }));
  };

  render () {
    const attributes = {
      onChange: this.handleLanguageChange,
      className: 'input-super-large',
      placeholder: translate('search_verb'),
      clearable: false,
      searchable: true
    };
    if (this.props.isAsync) {
      return <Select.Async {...attributes} cache={false} facet={this.props.facet} loadOptions={this.loadOptions}/>;
    } else {
      return <Select {...attributes} options={this.filterOptions(this.props.getOptions())}/>;
    }
  }
}
