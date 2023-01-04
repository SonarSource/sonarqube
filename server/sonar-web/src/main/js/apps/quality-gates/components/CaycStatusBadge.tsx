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
import * as React from 'react';
import Tooltip from '../../../components/controls/Tooltip';
import AlertErrorIcon from '../../../components/icons/AlertErrorIcon';
import AlertWarnIcon from '../../../components/icons/AlertWarnIcon';
import CheckIcon from '../../../components/icons/CheckIcon';
import { translate } from '../../../helpers/l10n';
import { BadgeTarget, QGBadgeType } from '../../../types/quality-gates';
import CaycBadgeTooltip from './CaycBadgeTooltip';

interface Props {
  className?: string;
  target?: BadgeTarget;
  type: QGBadgeType;
}

const iconForType = {
  [QGBadgeType.Ok]: CheckIcon,
  [QGBadgeType.Missing]: AlertErrorIcon,
  [QGBadgeType.Weak]: AlertWarnIcon,
};

const getIcon = (type: QGBadgeType) => iconForType[type];

export default function CaycStatusBadge({
  className,
  target = BadgeTarget.Condition,
  type,
}: Props) {
  const Icon = getIcon(type);

  return (
    <Tooltip overlay={<CaycBadgeTooltip badgeType={type} target={target} />}>
      <div className={classNames(`badge qg-cayc-${type}-badge display-flex-center`, className)}>
        <Icon className="spacer-right" />
        <span>{translate('quality_gates.cayc_condition', type)}</span>
      </div>
    </Tooltip>
  );
}
