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
import { Checkbox, HelperHintIcon, InputSearch } from 'design-system';
import * as React from 'react';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import { translate } from '../../../helpers/l10n';
import { Query } from '../utils';

interface Props {
  onSearch: (search: string) => void;
  onToggleDeprecated: () => void;
  onToggleInternal: () => void;
  query: Query;
}

export default function Search(props: Props) {
  const { query, onToggleInternal, onToggleDeprecated } = props;

  return (
    <div>
      <div>
        <InputSearch
          onChange={props.onSearch}
          placeholder={translate('api_documentation.search')}
          value={query.search}
        />
      </div>

      <div className="sw-flex sw-items-center sw-mt-4">
        <Checkbox checked={query.internal} onCheck={onToggleInternal}>
          <span className="sw-ml-2">{translate('api_documentation.show_internal')}</span>
        </Checkbox>
        <HelpTooltip className="sw-ml-2" overlay={translate('api_documentation.internal_tooltip')}>
          <HelperHintIcon />
        </HelpTooltip>
      </div>

      <div className="sw-flex sw-items-center sw-mt-2">
        <Checkbox checked={query.deprecated} onCheck={onToggleDeprecated}>
          <span className="sw-ml-2">{translate('api_documentation.show_deprecated')}</span>
        </Checkbox>
        <HelpTooltip
          className="sw-ml-2"
          overlay={translate('api_documentation.deprecation_tooltip')}
        >
          <HelperHintIcon />
        </HelpTooltip>
      </div>
    </div>
  );
}
