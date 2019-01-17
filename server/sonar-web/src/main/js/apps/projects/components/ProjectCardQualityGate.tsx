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
import Level from '../../../components/ui/Level';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import Tooltip from '../../../components/controls/Tooltip';
import { formatMeasure } from '../../../helpers/measures';
import { translateWithParameters, translate } from '../../../helpers/l10n';

interface Props {
  status?: string;
}

export default function ProjectCardQualityGate({ status }: Props) {
  if (!status) {
    return null;
  }

  const tooltip = translateWithParameters(
    'overview.quality_gate_x',
    formatMeasure(status, 'LEVEL')
  );

  return (
    <div className="project-card-quality-gate big-spacer-left">
      <Tooltip overlay={tooltip}>
        <div className="project-card-measure-inner">
          <Level level={status} small={true} />
          {status === 'WARN' && (
            <HelpTooltip
              className="little-spacer-left"
              overlay={translate('quality_gates.conditions.warning.tootlip')}
            />
          )}
        </div>
      </Tooltip>
    </div>
  );
}
