/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import Link from '../../../components/common/Link';
import { translate } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
import { getRulesUrl } from '../../../helpers/urls';

interface Props {
  count: number | null;
  qprofile: string;
  total: number | null;
  organization: string;
}

export default function ProfileRulesRowTotal(props: Props) {
  const {organization} = props;
  const activeRulesUrl = getRulesUrl({ qprofile: props.qprofile, activation: 'true' }, organization);
  const inactiveRulesUrl = getRulesUrl({ qprofile: props.qprofile, activation: 'false'}, organization);
  let inactiveCount = null;
  if (props.count != null && props.total != null) {
    inactiveCount = props.total - props.count;
  }

  return (
    <tr>
      <td>
        <strong>{translate('total')}</strong>
      </td>
      <td className="thin nowrap text-right">
        {props.count != null && (
          <Link to={activeRulesUrl}>
            <strong>{formatMeasure(props.count, 'SHORT_INT', null)}</strong>
          </Link>
        )}
      </td>
      <td className="thin nowrap text-right">
        {inactiveCount != null &&
          (inactiveCount > 0 ? (
            <Link className="small" to={inactiveRulesUrl}>
              <strong>{formatMeasure(inactiveCount, 'SHORT_INT', null)}</strong>
            </Link>
          ) : (
            <span className="note">0</span>
          ))}
      </td>
    </tr>
  );
}
