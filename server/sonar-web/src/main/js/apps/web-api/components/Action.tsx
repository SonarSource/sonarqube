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
import { Link } from 'react-router';
import * as classNames from 'classnames';
import Params from './Params';
import ResponseExample from './ResponseExample';
import ActionChangelog from './ActionChangelog';
import DeprecatedBadge from './DeprecatedBadge';
import InternalBadge from './InternalBadge';
import { getActionKey } from '../utils';
import LinkIcon from '../../../components/icons-components/LinkIcon';
import { Action as ActionType, Domain as DomainType } from '../../../api/web-api';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  action: ActionType;
  domain: DomainType;
  showDeprecated: boolean;
  showInternal: boolean;
}

interface State {
  showChangelog: boolean;
  showParams: boolean;
  showResponse: boolean;
}

export default class Action extends React.PureComponent<Props, State> {
  state: State = {
    showChangelog: false,
    showParams: false,
    showResponse: false
  };

  handleShowParamsClick = (e: React.SyntheticEvent<HTMLElement>) => {
    e.preventDefault();
    this.setState({
      showChangelog: false,
      showResponse: false,
      showParams: !this.state.showParams
    });
  };

  handleShowResponseClick = (e: React.SyntheticEvent<HTMLElement>) => {
    e.preventDefault();
    this.setState({
      showChangelog: false,
      showParams: false,
      showResponse: !this.state.showResponse
    });
  };

  handleChangelogClick = (e: React.SyntheticEvent<HTMLElement>) => {
    e.preventDefault();
    this.setState({
      showChangelog: !this.state.showChangelog,
      showParams: false,
      showResponse: false
    });
  };

  renderTabs() {
    const { action } = this.props;
    const { showChangelog, showParams, showResponse } = this.state;

    if (action.params || action.hasResponseExample || action.changelog.length > 0) {
      return (
        <ul className="web-api-action-actions tabs">
          {action.params && (
            <li>
              <a
                className={classNames({ selected: showParams })}
                href="#"
                onClick={this.handleShowParamsClick}>
                {translate('api_documentation.parameters')}
              </a>
            </li>
          )}

          {action.hasResponseExample && (
            <li>
              <a
                className={classNames({ selected: showResponse })}
                href="#"
                onClick={this.handleShowResponseClick}>
                {translate('api_documentation.response_example')}
              </a>
            </li>
          )}

          {action.changelog.length > 0 && (
            <li>
              <a
                className={classNames({ selected: showChangelog })}
                href="#"
                onClick={this.handleChangelogClick}>
                {translate('api_documentation.changelog')}
              </a>
            </li>
          )}
        </ul>
      );
    }

    return null;
  }

  render() {
    const { action, domain } = this.props;
    const { showChangelog, showParams, showResponse } = this.state;
    const verb = action.post ? 'POST' : 'GET';
    const actionKey = getActionKey(domain.path, action.key);

    return (
      <div id={actionKey} className="boxed-group">
        <header className="web-api-action-header boxed-group-header">
          <Link
            to={{ pathname: '/web_api/' + actionKey }}
            className="spacer-right link-no-underline">
            <LinkIcon />
          </Link>

          <h3 className="web-api-action-title">
            {verb}&nbsp;{actionKey}
          </h3>

          {action.internal && (
            <span className="spacer-left">
              <InternalBadge />
            </span>
          )}

          {action.since && (
            <span className="spacer-left badge">
              {translateWithParameters('since_x', action.since)}
            </span>
          )}

          {action.deprecatedSince && (
            <span className="spacer-left">
              <DeprecatedBadge since={action.deprecatedSince} />
            </span>
          )}
        </header>

        <div className="boxed-group-inner">
          <div
            className="web-api-action-description markdown"
            dangerouslySetInnerHTML={{ __html: action.description }}
          />

          {this.renderTabs()}

          {showParams &&
            action.params && (
              <Params
                params={action.params}
                showDeprecated={this.props.showDeprecated}
                showInternal={this.props.showInternal}
              />
            )}

          {showResponse &&
            action.hasResponseExample && <ResponseExample domain={domain} action={action} />}

          {showChangelog && <ActionChangelog changelog={action.changelog} />}
        </div>
      </div>
    );
  }
}
