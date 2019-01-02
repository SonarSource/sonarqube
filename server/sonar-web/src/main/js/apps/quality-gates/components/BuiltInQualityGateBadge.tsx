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
import * as classNames from 'classnames';
import DocTooltip from '../../../components/docs/DocTooltip';
import { translate } from '../../../helpers/l10n';

interface Props {
  className?: string;
}

export default function BuiltInQualityGateBadge({ className }: Props) {
  const badge = (
    <div className={classNames('outline-badge', className)}>
      {translate('quality_gates.built_in')}
    </div>
  );

  return (
    <DocTooltip
      doc={import(/* webpackMode: "eager" */ 'Docs/tooltips/quality-gates/built-in-quality-gate.md')}>
      {badge}
    </DocTooltip>
  );
}
