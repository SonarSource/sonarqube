/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import classNames from 'classnames';
import Tooltip from '../controls/Tooltip';
import { translate } from '../../helpers/l10n';

type Props = {
  className?: string,
  tooltipPlacement?: string
};

export default function PrivateBadge({ className, tooltipPlacement = 'bottom' }: Props) {
  return (
    <Tooltip overlay={translate('visibility.private.description')} placement={tooltipPlacement}>
      <div className={classNames('private-badge', className)}>
        {translate('visibility.private')}
      </div>
    </Tooltip>
  );
}
