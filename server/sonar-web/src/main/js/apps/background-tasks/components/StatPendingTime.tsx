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
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { formatMeasure } from 'sonar-ui-common/helpers/measures';

// Do not display the pending time for values smaller than this threshold (in ms)
const MIN_PENDING_TIME_THRESHOLD = 1000;

export interface Props {
  className?: string;
  component?: Pick<T.Component, 'key'>;
  pendingCount?: number;
  pendingTime?: number;
}

export default function StatPendingTime({ className, pendingCount, pendingTime }: Props) {
  if (!pendingTime || !pendingCount || pendingTime < MIN_PENDING_TIME_THRESHOLD) {
    return null;
  }
  return (
    <span className={className}>
      <span className="emphasised-measure">{formatMeasure(pendingTime, 'MILLISEC')}</span>
      <span className="little-spacer-left">{translate('background_tasks.pending_time')}</span>
      <HelpTooltip
        className="little-spacer-left"
        overlay={translate('background_tasks.pending_time.description')}
      />
    </span>
  );
}
