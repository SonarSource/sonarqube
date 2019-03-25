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
import * as React from 'react';
import Select from '../../../components/controls/Select';
import { translate } from '../../../helpers/l10n';
import { RawQuery } from '../../../helpers/query';

interface Props {
  isFavorite?: boolean;
  isLoading?: boolean;
  onInputChange?: (query: string) => void;
  onOpen?: () => void;
  onQueryChange: (change: RawQuery) => void;
  options: Array<{ label: string; value: string }>;
  organization?: { key: string };
  property: string;
  query: T.Dict<any>;
}

export default class SearchableFilterFooter extends React.PureComponent<Props> {
  handleOptionChange = ({ value }: { value: string }) => {
    const urlOptions = (this.props.query[this.props.property] || []).concat(value).join(',');
    this.props.onQueryChange({ [this.props.property]: urlOptions });
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
