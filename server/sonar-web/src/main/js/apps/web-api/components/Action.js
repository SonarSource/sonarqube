/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React from 'react';
import { Link } from 'react-router';

import { getActionKey } from '../utils';
import Params from './Params';
import ResponseExample from './ResponseExample';
import DeprecatedBadge from './DeprecatedBadge';
import InternalBadge from './InternalBadge';
import { TooltipsContainer } from '../../../components/mixins/tooltips-mixin';

export default class Action extends React.Component {
  static propTypes = {
    showInternal: React.PropTypes.bool
  };

  state = {
    showParams: false,
    showResponse: false
  };

  handleShowParamsClick (e) {
    e.preventDefault();
    this.refs.toggleParameters.blur();
    this.setState({ showResponse: false, showParams: !this.state.showParams });
  }

  handleShowResponseClick (e) {
    e.preventDefault();
    this.refs.toggleResponse.blur();
    this.setState({ showParams: false, showResponse: !this.state.showResponse });
  }

  render () {
    const { action, domain } = this.props;
    const { showParams, showResponse } = this.state;
    const verb = action.post ? 'POST' : 'GET';
    const actionKey = getActionKey(domain.path, action.key);

    return (
        <div id={actionKey} className="web-api-action">
          <TooltipsContainer>
            <header className="web-api-action-header">
              <Link
                  to={{ pathname: '/web_api/' + actionKey }}
                  className="spacer-right icon-link"/>

              <h3 className="web-api-action-title">
                {verb}&nbsp;{actionKey}
              </h3>

              {action.internal && (
                  <span className="spacer-left">
                  <InternalBadge/>
                </span>
              )}

              {action.since && (
                  <span className="spacer-left badge">since {action.since}</span>
              )}

              {action.deprecatedSince && (
                  <span className="spacer-left">
                  <DeprecatedBadge since={action.deprecatedSince}/>
                </span>
              )}
            </header>
          </TooltipsContainer>

          <div
              className="web-api-action-description markdown"
              dangerouslySetInnerHTML={{ __html: action.description }}/>

          {(action.params || action.hasResponseExample) && (
              <ul className="web-api-action-actions list-inline">
                {action.params && (
                    <li>
                      <a
                          ref="toggleParameters"
                          onClick={this.handleShowParamsClick.bind(this)}
                          href="#">
                        {showParams ? 'Hide Parameters' : 'Show Parameters'}
                      </a>
                    </li>
                )}

                {action.hasResponseExample && (
                    <li>
                      <a
                          ref="toggleResponse"
                          onClick={this.handleShowResponseClick.bind(this)}
                          href="#">
                        {showResponse ? 'Hide Response Example' : 'Show Response Example'}
                      </a>
                    </li>
                )}
              </ul>
          )}

          {showParams && action.params && <Params params={action.params} showInternal={this.props.showInternal}/>}

          {showResponse && action.hasResponseExample && <ResponseExample domain={domain} action={action}/>}
        </div>
    );
  }
}
