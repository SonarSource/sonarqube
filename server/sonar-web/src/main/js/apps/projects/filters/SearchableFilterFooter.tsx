/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { Dict, RawQuery } from '../../../types/types';

interface Props {
  isLoading?: boolean;
  onInputChange?: (query: string) => void;
  onOpen?: () => void;
  onQueryChange: (change: RawQuery) => void;
  options: Array<{ label: string; value: string }>;
  property: string;
  query: Dict<any>;
}

export default function SearchableFilterFooter(props: Props) {
  const { property, query } = props;

  const handleOptionChange = ({ value }: { value: string }) => {
    if (value) {
      const urlOptions = (query[property] || []).concat(value).join(',');
      props.onQueryChange({ [property]: urlOptions });
    }
  };

  return (
    <div className="search-navigator-facet-footer projects-facet-footer">
      <Select
        aria-label={translate('projects.facets.search', property)}
        className="input-super-large"
        controlShouldRenderValue={false}
        isLoading={props.isLoading}
        onChange={handleOptionChange}
        onInputChange={props.onInputChange}
        onMenuOpen={props.onOpen}
        options={props.options}
        placeholder={translate('search_verb')}
      />
    </div>
  );
}
