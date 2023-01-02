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
import { Condition } from '../../../types/types';
import { isCaycWeakCondition } from '../utils';
import CaycBadgeTooltip from './CaycBadgeTooltip';

interface Props {
  className?: string;
  isMissingCondition?: boolean;
  condition: Condition;
  isCaycModal?: boolean;
}

export default function CaycStatusBadge({
  className,
  isMissingCondition,
  condition,
  isCaycModal,
}: Props) {
  if (isMissingCondition && !isCaycModal) {
    return (
      <Tooltip overlay={<CaycBadgeTooltip badgeType="missing" />}>
        <div className={classNames('badge qg-cayc-missing-badge display-flex-center', className)}>
          <AlertErrorIcon className="spacer-right" />
          <span>{translate('quality_gates.cayc_condition.missing')}</span>
        </div>
      </Tooltip>
    );
  } else if (isCaycWeakCondition(condition) && !isCaycModal) {
    return (
      <Tooltip overlay={<CaycBadgeTooltip badgeType="weak" />}>
        <div className={classNames('badge qg-cayc-weak-badge display-flex-center', className)}>
          <AlertWarnIcon className="spacer-right" />
          <span>{translate('quality_gates.cayc_condition.weak')}</span>
        </div>
      </Tooltip>
    );
  }
  return (
    <Tooltip overlay={<CaycBadgeTooltip badgeType="ok" />}>
      <div className={classNames('badge qg-cayc-ok-badge display-flex-center', className)}>
        <CheckIcon className="spacer-right" />
        <span>{translate('quality_gates.cayc_condition.ok')}</span>
      </div>
    </Tooltip>
  );
}
