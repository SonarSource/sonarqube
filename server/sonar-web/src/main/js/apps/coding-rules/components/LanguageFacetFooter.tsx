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
import Select from '../../../components/controls/Select';
import { translate } from '../../../helpers/l10n';

type Option = { label: string; value: string };

interface Props {
  referencedLanguages: { [language: string]: { key: string; name: string } };
  onSelect: (value: string) => void;
}

export default class LanguageFacetFooter extends React.PureComponent<Props> {
  handleChange = (option: Option) => this.props.onSelect(option.value);

  render() {
    const options = Object.values(this.props.referencedLanguages).map(language => ({
      label: language.name,
      value: language.key
    }));

    return (
      <div className="search-navigator-facet-footer">
        <Select
          className="input-super-large"
          clearable={false}
          noResultsText={translate('select2.noMatches')}
          onChange={this.handleChange}
          options={options}
          placeholder={translate('search.search_for_languages')}
          searchable={true}
        />
      </div>
    );
  }
}
