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
import Checkbox from '../../../components/controls/Checkbox';
import HelpIcon from '../../../components/icons-components/HelpIcon';
import Tooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';
import SearchBox from '../../../components/controls/SearchBox';

interface Props {
  showDeprecated: boolean;
  showInternal: boolean;
  onSearch: (search: string) => void;
  onToggleInternal: () => void;
  onToggleDeprecated: () => void;
}

export default function Search(props: Props) {
  const { showInternal, showDeprecated, onToggleInternal, onToggleDeprecated } = props;

  return (
    <div className="web-api-search">
      <div>
        <SearchBox onChange={props.onSearch} placeholder={translate('api_documentation.search')} />
      </div>

      <div className="big-spacer-top">
        <Checkbox checked={showInternal} onCheck={onToggleInternal}>
          <span className="little-spacer-left">{translate('api_documentation.show_internal')}</span>
        </Checkbox>
        <Tooltip overlay={translate('api_documentation.internal_tooltip')} placement="right">
          <span>
            <HelpIcon className="spacer-left text-info" />
          </span>
        </Tooltip>
      </div>

      <div className="spacer-top">
        <Checkbox checked={showDeprecated} onCheck={onToggleDeprecated}>
          <span className="little-spacer-left">
            {translate('api_documentation.show_deprecated')}
          </span>
        </Checkbox>
        <Tooltip overlay={translate('api_documentation.deprecation_tooltip')} placement="right">
          <span>
            <HelpIcon className="spacer-left text-info" />
          </span>
        </Tooltip>
      </div>
    </div>
  );
}
