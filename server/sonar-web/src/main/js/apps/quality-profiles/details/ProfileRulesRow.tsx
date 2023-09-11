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
import { ContentCell, Link, Note, NumericalCell, TableRow } from 'design-system';
import * as React from 'react';
import IssueTypeIcon from '../../../components/icons/IssueTypeIcon';
import { translate } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
import { isDefined } from '../../../helpers/types';
import { getRulesUrl } from '../../../helpers/urls';
import { MetricType } from '../../../types/metrics';

interface Props {
  count: number | null;
  qprofile: string;
  total: number | null;
  type?: string;
}

export default function ProfileRulesRowOfType(props: Props) {
  const activeRulesUrl = getRulesUrl({
    qprofile: props.qprofile,
    activation: 'true',
    types: props.type,
  });
  const inactiveRulesUrl = getRulesUrl({
    qprofile: props.qprofile,
    activation: 'false',
    types: props.type,
  });
  let inactiveCount = null;
  if (props.count != null && props.total != null) {
    inactiveCount = props.total - props.count;
  }

  return (
    <TableRow>
      <ContentCell>
        {props.type ? (
          <>
            <IssueTypeIcon className="sw-mr-1" query={props.type} />
            {translate('issue.type', props.type, 'plural')}
          </>
        ) : (
          translate('total')
        )}
      </ContentCell>
      <NumericalCell>
        {isDefined(props.count) && (
          <Link to={activeRulesUrl}>{formatMeasure(props.count, MetricType.ShortInteger)}</Link>
        )}
      </NumericalCell>
      <NumericalCell>
        {isDefined(inactiveCount) &&
          (inactiveCount > 0 ? (
            <Link to={inactiveRulesUrl}>
              {formatMeasure(inactiveCount, MetricType.ShortInteger)}
            </Link>
          ) : (
            <Note>0</Note>
          ))}
      </NumericalCell>
    </TableRow>
  );
}
