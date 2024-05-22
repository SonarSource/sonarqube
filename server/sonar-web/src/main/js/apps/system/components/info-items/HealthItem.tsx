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
import StatusIndicator from '../../../../components/common/StatusIndicator';
import Tooltip from '../../../../components/controls/Tooltip';
import { translateWithParameters } from '../../../../helpers/l10n';
import { HealthTypes } from '../../../../types/types';
import HealthCauseItem from './HealthCauseItem';

interface Props {
  name?: string;
  className?: string;
  health: HealthTypes;
  healthCauses?: string[];
}

export default function HealthItem({ className, name, health, healthCauses }: Readonly<Props>) {
  const hasHealthCauses = healthCauses && healthCauses.length > 0 && health !== HealthTypes.GREEN;

  const statusIndicator = <StatusIndicator color={health} />;
  return (
    <div className={classNames('sw-flex sw-items-center', className)}>
      {hasHealthCauses &&
        healthCauses.map((cause) => (
          <HealthCauseItem className="sw-mr-2" health={health} healthCause={cause} key={cause} />
        ))}

      <Tooltip
        content={name ? translateWithParameters('system.current_health_of_x', name) : undefined}
      >
        <span>{statusIndicator}</span>
      </Tooltip>
    </div>
  );
}
