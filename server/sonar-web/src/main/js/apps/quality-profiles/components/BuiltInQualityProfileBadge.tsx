/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import * as classNames from 'classnames';
import * as React from 'react';
import { translate } from 'sonar-ui-common/helpers/l10n';
import DocTooltip from '../../../components/docs/DocTooltip';

interface Props {
  className?: string;
  tooltip?: boolean;
}

export default function BuiltInQualityProfileBadge({ className, tooltip = true }: Props) {
  const badge = (
    <div className={classNames('badge badge-info', className)}>
      {translate('quality_profiles.built_in')}
    </div>
  );

  if (tooltip) {
    return (
      <DocTooltip
        doc={import(
          /* webpackMode: "eager" */ 'Docs/tooltips/quality-profiles/built-in-quality-profile.md'
        )}>
        {badge}
      </DocTooltip>
    );
  }
  return badge;
}
