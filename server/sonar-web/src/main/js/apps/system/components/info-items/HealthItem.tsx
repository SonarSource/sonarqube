/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import HealthCauseItem from './HealthCauseItem';
import StatusIndicator from '../../../../components/common/StatusIndicator';
import Tooltip from '../../../../components/controls/Tooltip';
import { HealthType } from '../../../../api/system';
import { translateWithParameters } from '../../../../helpers/l10n';

interface Props {
  biggerHealth?: boolean;
  name?: string;
  className?: string;
  health: HealthType;
  healthCauses?: string[];
}

export default function HealthItem({ biggerHealth, className, name, health, healthCauses }: Props) {
  const hasHealthCauses = healthCauses && healthCauses.length > 0 && health !== HealthType.GREEN;
  const statusIndicator = (
    <StatusIndicator color={health.toLowerCase()} size={biggerHealth ? 'big' : undefined} />
  );
  return (
    <div className={classNames('system-info-health-info', className)}>
      {hasHealthCauses &&
        healthCauses!.map((cause, idx) => (
          <HealthCauseItem key={idx} className="spacer-right" health={health} healthCause={cause} />
        ))}
      {name ? (
        <Tooltip
          overlay={translateWithParameters('system.current_health_of_x', name)}
          placement="left">
          <span>{statusIndicator}</span>
        </Tooltip>
      ) : (
        statusIndicator
      )}
    </div>
  );
}
