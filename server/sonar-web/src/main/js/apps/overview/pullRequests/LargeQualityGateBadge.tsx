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
import { Link } from 'react-router';
import { FormattedMessage } from 'react-intl';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import HelpIcon from '../../../components/icons-components/HelpIcon';
import { getQualityGateUrl, getQualityGatesUrl } from '../../../helpers/urls';
import { isSonarCloud } from '../../../helpers/system';
import { translate } from '../../../helpers/l10n';
import { transparentWhite } from '../../../app/theme';

interface Props {
  component: T.Component;
  level?: T.Status;
}

export default function LargeQualityGateBadge({ component, level }: Props) {
  const success = level === 'OK';

  let path;
  if (isSonarCloud()) {
    path =
      component.qualityGate === undefined
        ? getQualityGatesUrl(component.organization)
        : getQualityGateUrl(component.qualityGate.key, component.organization);
  } else {
    path =
      component.qualityGate === undefined
        ? getQualityGatesUrl()
        : getQualityGateUrl(component.qualityGate.key);
  }

  return (
    <div
      className={classNames('quality-gate-badge-large small', {
        failed: !success,
        success
      })}>
      <div className="display-flex-center">
        <span>{translate('overview.on_new_code_long')}</span>

        <HelpTooltip
          className="little-spacer-left"
          overlay={
            <FormattedMessage
              defaultMessage={translate('overview.quality_gate.conditions_on_new_code')}
              id="overview.quality_gate.conditions_on_new_code"
              values={{
                link: <Link to={path}>{translate('overview.quality_gate')}</Link>
              }}
            />
          }>
          <HelpIcon fill={transparentWhite} size={12} />
        </HelpTooltip>
      </div>
      {level !== undefined && (
        <h4 className="huge-spacer-top huge">{translate('metric.level', level)}</h4>
      )}
    </div>
  );
}
