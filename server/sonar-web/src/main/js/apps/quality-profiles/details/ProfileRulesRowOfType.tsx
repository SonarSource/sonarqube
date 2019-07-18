/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import IssueTypeIcon from 'sonar-ui-common/components/icons/IssueTypeIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { formatMeasure } from 'sonar-ui-common/helpers/measures';
import { getRulesUrl } from '../../../helpers/urls';

interface Props {
  count: number | null;
  organization: string | null;
  qprofile: string;
  total: number | null;
  type: string;
}

export default function ProfileRulesRowOfType(props: Props) {
  const activeRulesUrl = getRulesUrl(
    { qprofile: props.qprofile, activation: 'true', types: props.type },
    props.organization
  );
  const inactiveRulesUrl = getRulesUrl(
    { qprofile: props.qprofile, activation: 'false', types: props.type },
    props.organization
  );
  let inactiveCount = null;
  if (props.count != null && props.total != null) {
    inactiveCount = props.total - props.count;
  }

  return (
    <tr>
      <td>
        <span>
          <IssueTypeIcon className="little-spacer-right" query={props.type} />
          {translate('issue.type', props.type, 'plural')}
        </span>
      </td>
      <td className="thin nowrap text-right">
        {props.count != null && (
          <Link to={activeRulesUrl}>{formatMeasure(props.count, 'SHORT_INT', null)}</Link>
        )}
      </td>
      <td className="thin nowrap text-right">
        {inactiveCount != null &&
          (inactiveCount > 0 ? (
            <Link className="small text-muted" to={inactiveRulesUrl}>
              {formatMeasure(inactiveCount, 'SHORT_INT', null)}
            </Link>
          ) : (
            <span className="note text-muted">0</span>
          ))}
      </td>
    </tr>
  );
}
