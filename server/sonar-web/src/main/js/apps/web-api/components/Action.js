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
import { Link } from 'react-router';
import classNames from 'classnames';
import { getActionKey } from '../utils';
import Params from './Params';
import ResponseExample from './ResponseExample';
import ActionChangelog from './ActionChangelog';
import DeprecatedBadge from './DeprecatedBadge';
import InternalBadge from './InternalBadge';
import { TooltipsContainer } from '../../../components/mixins/tooltips-mixin';
import type { Action as ActionType, Domain as DomainType } from '../../../api/web-api';

type Props = {
  action: ActionType,
  domain: DomainType,
  showDeprecated: boolean,
  showInternal: boolean
};

type State = {
  showChangelog: boolean,
  showParams: boolean,
  showResponse: boolean
};

export default class Action extends React.PureComponent {
  props: Props;

  state: State = {
    showChangelog: false,
    showParams: false,
    showResponse: false
  };

  handleShowParamsClick = (e: SyntheticInputEvent) => {
    e.preventDefault();
    this.setState({
      showChangelog: false,
      showResponse: false,
      showParams: !this.state.showParams
    });
  };

  handleShowResponseClick = (e: SyntheticInputEvent) => {
    e.preventDefault();
    this.setState({
      showChangelog: false,
      showParams: false,
      showResponse: !this.state.showResponse
    });
  };

  handleChangelogClick = (e: SyntheticInputEvent) => {
    e.preventDefault();
    this.setState({
      showChangelog: !this.state.showChangelog,
      showParams: false,
      showResponse: false
    });
  };

  render() {
    const { action, domain } = this.props;
    const { showChangelog, showParams, showResponse } = this.state;
    const verb = action.post ? 'POST' : 'GET';
    const actionKey = getActionKey(domain.path, action.key);

    return (
      <div id={actionKey} className="web-api-action">
        <TooltipsContainer>
          <header className="web-api-action-header">
            <Link to={{ pathname: '/web_api/' + actionKey }} className="spacer-right icon-link" />

            <h3 className="web-api-action-title">
              {verb}&nbsp;{actionKey}
            </h3>

            {action.internal &&
              <span className="spacer-left">
                <InternalBadge />
              </span>}

            {action.since && <span className="spacer-left badge">since {action.since}</span>}

            {action.deprecatedSince &&
              <span className="spacer-left">
                <DeprecatedBadge since={action.deprecatedSince} />
              </span>}
          </header>
        </TooltipsContainer>

        <div
          className="web-api-action-description markdown"
          dangerouslySetInnerHTML={{ __html: action.description }}
        />

        {(action.params || action.hasResponseExample) &&
          <ul className="web-api-action-actions tabs">
            {action.params &&
              <li>
                <a
                  className={classNames({ selected: showParams })}
                  href="#"
                  onClick={this.handleShowParamsClick}>
                  Parameters
                </a>
              </li>}

            {action.hasResponseExample &&
              <li>
                <a
                  className={classNames({ selected: showResponse })}
                  href="#"
                  onClick={this.handleShowResponseClick}>
                  Response Example
                </a>
              </li>}

            {action.changelog.length > 0 &&
              <li>
                <a
                  className={classNames({ selected: showChangelog })}
                  href="#"
                  onClick={this.handleChangelogClick}>
                  Changelog
                </a>
              </li>}
          </ul>}

        {showParams &&
          action.params &&
          <Params
            params={action.params}
            showDeprecated={this.props.showDeprecated}
            showInternal={this.props.showInternal}
          />}

        {showResponse &&
          action.hasResponseExample &&
          <ResponseExample domain={domain} action={action} />}

        {showChangelog && <ActionChangelog changelog={action.changelog} />}
      </div>
    );
  }
}
