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

import * as React from 'react';
import Tooltip from '../../components/controls/Tooltip';

export interface DisableableSelectOptionProps {
  className?: string;
  disableTooltipOverlay: () => React.ReactNode;
  disabledReason?: string;
  option: { isDisabled?: boolean; label?: string; value?: string | number | boolean };
}

export default function DisableableSelectOption(props: DisableableSelectOptionProps) {
  const { option, disableTooltipOverlay, disabledReason, className = '' } = props;
  const label = option.label || option.value;
  return option.isDisabled ? (
    <Tooltip content={disableTooltipOverlay()} side="left">
      <span className={className}>
        {label}
        {disabledReason !== undefined && <em className="small sw-ml-1">({disabledReason})</em>}
      </span>
    </Tooltip>
  ) : (
    <span className={className}>{label}</span>
  );
}
