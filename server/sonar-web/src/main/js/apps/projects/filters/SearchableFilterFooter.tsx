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
import * as Select from 'react-select';
import * as PropTypes from 'prop-types';
import { getFilterUrl } from './utils';
import { translate } from '../../../helpers/l10n';

interface Props {
  property: string;
  query: { [x: string]: any };
  options: Array<{ label: string; value: string }>;
  onInputChange?: (query: string) => void;
  onOpen?: () => void;
  isLoading?: boolean;
  isFavorite?: boolean;
  organization?: { key: string };
}

export default class SearchableFilterFooter extends React.PureComponent<Props> {
  static contextTypes = {
    router: PropTypes.object.isRequired
  };

  handleOptionChange = ({ value }: { value: string }) => {
    const urlOptions = (this.props.query[this.props.property] || []).concat(value).join(',');
    const path = getFilterUrl(this.props, { [this.props.property]: urlOptions });
    this.context.router.push(path);
  };

  render() {
    return (
      <div className="search-navigator-facet-footer projects-facet-footer">
        <Select
          className="input-super-large"
          clearable={false}
          isLoading={this.props.isLoading}
          onChange={this.handleOptionChange}
          onInputChange={this.props.onInputChange}
          onOpen={this.props.onOpen}
          options={this.props.options}
          placeholder={translate('search_verb')}
          searchable={true}
        />
      </div>
    );
  }
}
