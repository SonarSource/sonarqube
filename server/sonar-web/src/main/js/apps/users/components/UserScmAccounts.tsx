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
import { translateWithParameters } from '../../../helpers/l10n';

interface Props {
  scmAccounts: string[];
}

interface State {
  showMore: boolean;
}

const SCM_LIMIT = 3;

export default class UserScmAccounts extends React.PureComponent<Props, State> {
  state: State = { showMore: false };

  toggleShowMore = (evt: React.SyntheticEvent<HTMLAnchorElement>) => {
    evt.preventDefault();
    this.setState(state => ({ showMore: !state.showMore }));
  };

  render() {
    const { scmAccounts } = this.props;
    const limit = scmAccounts.length > SCM_LIMIT ? SCM_LIMIT - 1 : SCM_LIMIT;
    return (
      <ul>
        {scmAccounts.slice(0, limit).map((scmAccount, idx) => (
          <li key={idx} className="little-spacer-bottom">
            {scmAccount}
          </li>
        ))}
        {scmAccounts.length > SCM_LIMIT &&
          (this.state.showMore ? (
            scmAccounts.slice(limit).map((scmAccount, idx) => (
              <li key={idx + limit} className="little-spacer-bottom">
                {scmAccount}
              </li>
            ))
          ) : (
            <li className="little-spacer-bottom">
              <a className="js-user-more-scm" href="#" onClick={this.toggleShowMore}>
                {translateWithParameters('more_x', scmAccounts.length - limit)}
              </a>
            </li>
          ))}
      </ul>
    );
  }
}
