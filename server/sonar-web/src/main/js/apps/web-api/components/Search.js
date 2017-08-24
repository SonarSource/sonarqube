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
// @flow
import React from 'react';
import { debounce } from 'lodash';
import Checkbox from '../../../components/controls/Checkbox';
import HelpIcon from '../../../components/icons-components/HelpIcon';
import Tooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';

/*::
type Props = {
  showDeprecated: boolean,
  showInternal: boolean,
  onSearch: string => void,
  onToggleInternal: () => void,
  onToggleDeprecated: () => void
};
*/

/*::
type State = {
  query: string
};
*/

export default class Search extends React.PureComponent {
  /*:: actuallySearch: () => void; */
  /*:: props: Props; */
  /*:: state: State; */

  constructor(props /*: Props */) {
    super(props);
    this.state = { query: '' };
    this.actuallySearch = debounce(this.actuallySearch, 250);
  }

  handleSearch = (e /*: SyntheticInputEvent */) => {
    const { value } = e.target;
    this.setState({ query: value });
    this.actuallySearch();
  };

  actuallySearch = () => {
    const { onSearch } = this.props;
    onSearch(this.state.query);
  };

  render() {
    const { showInternal, showDeprecated, onToggleInternal, onToggleDeprecated } = this.props;

    return (
      <div className="web-api-search">
        <div>
          <i className="icon-search" />
          <input
            className="spacer-left input-large"
            type="search"
            value={this.state.query}
            placeholder={translate('search_verb')}
            onChange={this.handleSearch}
          />
        </div>

        <div className="big-spacer-top">
          <Checkbox checked={showInternal} onCheck={onToggleInternal}>
            <span className="little-spacer-left">
              {translate('api_documentation.show_deprecated')}
            </span>
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
              {translate('api_documentation.show_internal')}
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
}
