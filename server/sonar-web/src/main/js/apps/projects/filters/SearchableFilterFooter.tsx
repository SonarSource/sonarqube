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
import * as React from 'react';
import { components, InputProps, OptionProps } from 'react-select';
import Select from '../../../components/controls/Select';
import { translate } from '../../../helpers/l10n';
import { Dict, RawQuery } from '../../../types/types';

interface Props {
  isFavorite?: boolean;
  isLoading?: boolean;
  onInputChange?: (query: string) => void;
  onOpen?: () => void;
  onQueryChange: (change: RawQuery) => void;
  options: Array<{ label: string; value: string }>;
  property: string;
  query: Dict<any>;
}

export default class SearchableFilterFooter extends React.PureComponent<Props> {
  handleOptionChange = ({ value }: { value: string }) => {
    const urlOptions = (this.props.query[this.props.property] || []).concat(value).join(',');
    this.props.onQueryChange({ [this.props.property]: urlOptions });
  };

  optionRenderer = (props: OptionProps<{ value: string }, false>) => (
    <components.Option
      {...props}
      className={'Select-option ' + (props.isFocused ? 'is-focused' : '')}
    />
  );

  inputRenderer = (props: InputProps) => (
    <components.Input {...props} className="it__searchable-footer-select-input" />
  );

  render() {
    return (
      <div className="search-navigator-facet-footer projects-facet-footer">
        <Select
          className="input-super-large"
          isLoading={this.props.isLoading}
          onChange={this.handleOptionChange}
          onInputChange={this.props.onInputChange}
          onOpen={this.props.onOpen}
          components={{
            Option: this.optionRenderer,
            Input: this.inputRenderer
          }}
          options={this.props.options}
          placeholder={translate('search_verb')}
        />
      </div>
    );
  }
}
