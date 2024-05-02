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
import { QualityGateIndicator, TextError } from 'design-system';
import React from 'react';
import { useIntl } from 'react-intl';
import { Status } from '~sonar-aligned/types/common';
import { translate } from '../../../helpers/l10n';

interface Props {
  status: Status;
  failedConditionCount: number;
}

export default function QualityGateStatusHeader(props: Props) {
  const { status, failedConditionCount } = props;
  const intl = useIntl();

  return (
    <div className="sw-flex sw-items-center sw-mb-4">
      <QualityGateIndicator status={status} className="sw-mr-2" size="xl" />
      <div className="sw-flex sw-flex-col">
        <span className="sw-heading-lg">{translate('metric.level', status)}</span>
        {failedConditionCount > 0 && (
          <TextError
            className="sw-font-regular"
            text={intl.formatMessage(
              { id: 'overview.X_conditions_failed' },
              {
                conditions: <strong>{failedConditionCount}</strong>,
              },
            )}
          />
        )}
      </div>
    </div>
  );
}
