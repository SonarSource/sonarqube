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
import moment from 'moment';
import ChangesList from './ChangesList';
import { translate } from '../../../helpers/l10n';
import { getRulesUrl } from '../../../helpers/urls';

/*::
type Props = {
  events: Array<{
    action: string,
    authorName: string,
    date: string,
    params?: {},
    ruleKey: string,
    ruleName: string
  }>,
  organization: ?string
};
*/

export default class Changelog extends React.PureComponent {
  /*:: props: Props; */

  render() {
    let isEvenRow = false;

    const rows = this.props.events.map((event, index) => {
      const prev = index > 0 ? this.props.events[index - 1] : null;
      const isSameDate = prev != null && moment(prev.date).diff(event.date, 'seconds') < 10;
      const isBulkChange =
        prev != null &&
        isSameDate &&
        prev.authorName === event.authorName &&
        prev.action === event.action;

      if (!isBulkChange) {
        isEvenRow = !isEvenRow;
      }

      const className = 'js-profile-changelog-event ' + (isEvenRow ? 'even' : 'odd');

      return (
        <tr key={index} className={className}>
          <td className="thin nowrap">
            {!isBulkChange && moment(event.date).format('LLL')}
          </td>

          <td className="thin nowrap">
            {!isBulkChange &&
              (event.authorName
                ? <span>
                    {event.authorName}
                  </span>
                : <span className="note">System</span>)}
          </td>

          <td className="thin nowrap">
            {!isBulkChange && translate('quality_profiles.changelog', event.action)}
          </td>

          <td style={{ lineHeight: '1.5' }}>
            <Link to={getRulesUrl({ rule_key: event.ruleKey }, this.props.organization)}>
              {event.ruleName}
            </Link>
          </td>

          <td className="thin nowrap">
            <ChangesList changes={event.params} />
          </td>
        </tr>
      );
    });

    return (
      <table className="data zebra-hover">
        <thead>
          <tr>
            <th className="thin nowrap">
              {translate('date')} <i className="icon-sort-desc" />
            </th>
            <th className="thin nowrap">
              {translate('user')}
            </th>
            <th className="thin nowrap">
              {translate('action')}
            </th>
            <th>
              {translate('rule')}
            </th>
            <th className="thin nowrap">
              {translate('parameters')}
            </th>
          </tr>
        </thead>
        <tbody>
          {rows}
        </tbody>
      </table>
    );
  }
}
