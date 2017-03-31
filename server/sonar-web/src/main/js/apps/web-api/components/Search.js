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
import { TooltipsContainer } from '../../../components/mixins/tooltips-mixin';
import { translate } from '../../../helpers/l10n';

type Props = {
  showDeprecated: boolean,
  showInternal: boolean,
  onSearch: (string) => void,
  onToggleInternal: () => void,
  onToggleDeprecated: () => void
};

type State = {
  query: string
};

export default class Search extends React.PureComponent {
  actuallySearch: () => void;
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = { query: '' };
    this.actuallySearch = debounce(this.actuallySearch.bind(this), 250);
  }

  handleSearch(e: SyntheticInputEvent) {
    const { value } = e.target;
    this.setState({ query: value });
    this.actuallySearch();
  }

  actuallySearch() {
    const { onSearch } = this.props;
    onSearch(this.state.query);
  }

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
            onChange={this.handleSearch.bind(this)}
          />
        </div>

        <TooltipsContainer>
          <div className="big-spacer-top">
            <Checkbox checked={showInternal} onCheck={onToggleInternal} />
            {' '}
            <span
              style={{ cursor: 'pointer' }}
              title={translate('api_documentation.internal_tooltip')}
              tabIndex="0"
              role="checkbox"
              aria-checked={showInternal ? 'true' : 'false'}
              onClick={onToggleInternal}>
              Show Internal API
            </span>
            <i
              className="icon-help spacer-left"
              title={translate('api_documentation.internal_tooltip')}
              data-toggle="tooltip"
            />
          </div>
        </TooltipsContainer>

        <TooltipsContainer>
          <div className="spacer-top">
            <Checkbox checked={showDeprecated} onCheck={onToggleDeprecated} />
            {' '}
            <span
              style={{ cursor: 'pointer' }}
              title={translate('api_documentation.deprecation_tooltip')}
              tabIndex="0"
              role="checkbox"
              aria-checked={showDeprecated ? 'true' : 'false'}
              onClick={onToggleDeprecated}>
              Show Deprecated API
            </span>
            <i
              className="icon-help spacer-left"
              title={translate('api_documentation.deprecation_tooltip')}
              data-toggle="tooltip"
            />
          </div>
        </TooltipsContainer>
      </div>
    );
  }
}
