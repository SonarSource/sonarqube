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

import classNames from 'classnames';
import { CheckIcon, LightLabel } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';

export default function CaycConditionsListItem({
  index,
  last,
}: Readonly<{ index: number; last: boolean }>) {
  return (
    <li className={classNames('sw-flex', { 'sw-mb-2': !last })}>
      <CheckIcon className="sw-mr-1 sw-pt-1/2" />
      <LightLabel>{translate(`quality_gates.cayc.banner.list_item${index + 1}`)}</LightLabel>
    </li>
  );
}
