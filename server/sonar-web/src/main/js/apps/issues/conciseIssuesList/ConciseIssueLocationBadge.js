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
// @flow
import React from 'react';
import Tooltip from '../../../components/controls/Tooltip';
import LocationIndex from '../../../components/common/LocationIndex';
import { translateWithParameters } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';

/*::
type Props = {|
  count: number,
  onClick?: () => void,
  selected?: boolean
|};
*/

export default function ConciseIssueLocationBadge(props /*: Props */) {
  return (
    <Tooltip
      mouseEnterDelay={0.5}
      overlay={translateWithParameters(
        'issue.this_issue_involves_x_code_locations',
        formatMeasure(props.count)
      )}>
      <LocationIndex onClick={props.onClick} selected={props.selected}>
        {'+'}
        {props.count}
      </LocationIndex>
    </Tooltip>
  );
}
