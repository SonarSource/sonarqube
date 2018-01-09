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
import * as classNames from 'classnames';
import Tooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';

interface Props {
  className?: string;
  tooltip?: boolean;
}

export default function BuiltInQualityProfileBadge({ className, tooltip = true }: Props) {
  const badge = (
    <div className={classNames('outline-badge', className)}>
      {translate('quality_profiles.built_in')}
    </div>
  );

  const overlay = (
    <span>
      {translate('quality_profiles.built_in.description.1')}{' '}
      {translate('quality_profiles.built_in.description.2')}
    </span>
  );

  return tooltip ? (
    <Tooltip overlay={overlay} placement="right">
      {badge}
    </Tooltip>
  ) : (
    badge
  );
}
