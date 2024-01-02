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
import classNames from 'classnames';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { colors } from '../../../app/theme';
import Link from '../../../components/common/Link';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import HelpIcon from '../../../components/icons/HelpIcon';
import { translate } from '../../../helpers/l10n';
import { getQualityGatesUrl, getQualityGateUrl } from '../../../helpers/urls';
import { Component, Status } from '../../../types/types';

interface Props {
  component: Component;
  level?: Status;
}

export function LargeQualityGateBadge({ component, level }: Props) {
  const success = level === 'OK';

  const path =
    component.qualityGate === undefined
      ? getQualityGatesUrl()
      : getQualityGateUrl(component.qualityGate.key);

  return (
    <div
      className={classNames('overview-quality-gate-badge-large small', {
        failed: !success,
        success,
      })}
    >
      <div className="display-flex-center">
        <span>{translate('overview.on_new_code_long')}</span>

        <HelpTooltip
          className="little-spacer-left"
          overlay={
            <FormattedMessage
              defaultMessage={translate('overview.quality_gate.conditions_on_new_code')}
              id="overview.quality_gate.conditions_on_new_code"
              values={{
                link: <Link to={path}>{translate('overview.quality_gate')}</Link>,
              }}
            />
          }
        >
          <HelpIcon fill={colors.transparentWhite} size={12} />
        </HelpTooltip>
      </div>
      {level !== undefined && (
        <h3 className="huge-spacer-top huge">{translate('metric.level', level)}</h3>
      )}
    </div>
  );
}

export default React.memo(LargeQualityGateBadge);
