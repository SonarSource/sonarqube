/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { HelperHintIcon, StandoutLink } from 'design-system';
import * as React from 'react';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import { translate } from '../../../helpers/l10n';

export interface Props {
  failingCount?: number;
  onShowFailing: (e: React.SyntheticEvent<HTMLAnchorElement>) => void;
}

export default function StatStillFailing({ failingCount, onShowFailing }: Readonly<Props>) {
  if (failingCount === undefined) {
    return null;
  }

  return (
    <div className="sw-flex sw-items-center ">
      {failingCount > 0 ? (
        <StandoutLink
          className="sw-body-md-highlight sw-align-baseline"
          to="#"
          onClick={onShowFailing}
        >
          {failingCount}
        </StandoutLink>
      ) : (
        <span className="sw-body-md-highlight">{failingCount}</span>
      )}
      <span className="sw-ml-1">{translate('background_tasks.failures')}</span>
      <HelpTooltip className="sw-ml-1" overlay={translate('background_tasks.failing_count')}>
        <HelperHintIcon />
      </HelpTooltip>
    </div>
  );
}
