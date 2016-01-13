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
import Assignee from '../../../components/shared/assignee-helper';
import { getComponentIssuesUrl } from '../../../helpers/urls';
import { formatMeasure } from '../../../helpers/measures';


export default class extends React.Component {
  render () {
    let rows = this.props.assignees.map(s => {
      let params = { statuses: 'OPEN,REOPENED' };
      if (s.val) {
        params.assignees = s.val;
      } else {
        params.assigned = 'false';
      }
      let href = getComponentIssuesUrl(this.props.component.key, params);
      return <tr key={s.val}>
        <td>
          <Assignee user={s.user}/>
        </td>
        <td className="thin text-right">
          <a href={href}>{formatMeasure(s.count, 'SHORT_INT')}</a>
        </td>
      </tr>;
    });

    return <table className="data zebra">
      <tbody>{rows}</tbody>
    </table>;
  }
}
