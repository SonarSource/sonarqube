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
import { getFilterUrl } from './utils';
import { translate } from '../../../helpers/l10n';

type Props = {
  property: string,
  query: {},
  options: {} | [],
  getOptions: () => [{ label: string, value: string }],
  onInputChange?: (string) => void,
  onOpen?: (void) => void,
  value?: Array<string>,
  facet?: {},
  router?: { push: { path: string, query: {} } },
  isLoading?: boolean
};

export default class SearchableFilterFooter extends React.PureComponent {
  props: Props;

  handleOptionChange = ({ value }) => {
    const urlOptions = (this.props.value || []).concat(value).join(',');
    const path = getFilterUrl(this.props, { [this.props.property]: urlOptions });
    this.props.router.push(path);
  };

  render () {
    return (
      <Select
        onChange={this.handleOptionChange}
        className="input-super-large"
        placeholder={translate('search_verb')}
        clearable={false}
        searchable={true}
        onInputChange={this.props.onInputChange}
        onOpen={this.props.onOpen}
        isLoading={this.props.isLoading}
        options={this.props.getOptions()}/>
    );
  }
}
