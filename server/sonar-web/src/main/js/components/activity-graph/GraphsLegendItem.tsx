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

import styled from '@emotion/styled';
import { ButtonIcon, ButtonSize, IconWarning, IconX } from '@sonarsource/echoes-react';
import classNames from 'classnames';
import { themeBorder } from '~design-system';
import { translateWithParameters } from '../../helpers/l10n';
import { ChartLegend } from './ChartLegend';

interface Props {
  className?: string;
  index: number;
  metric: string;
  name: string;
  removeMetric?: (metric: string) => void;
  showWarning?: boolean;
}

export function GraphsLegendItem({
  className,
  index,
  metric,
  name,
  removeMetric,
  showWarning,
}: Readonly<Props>) {
  const isActionable = removeMetric !== undefined;

  return (
    <StyledLegendItem
      className={classNames('sw-px-2 sw-py-1 sw-rounded-2', className)}
      isActionable={isActionable}
    >
      {showWarning ? (
        <IconWarning color="echoes-color-icon-warning" className="sw-mr-2" />
      ) : (
        <ChartLegend className="sw-mr-2" index={index} />
      )}
      <span className="sw-typo-default" style={{ color: 'var(--echoes-color-text-subdued)' }}>
        {name}
      </span>
      {isActionable && (
        <ButtonIcon
          size={ButtonSize.Medium}
          Icon={IconX}
          ariaLabel={translateWithParameters('project_activity.graphs.custom.remove_metric', name)}
          className="sw-ml-2 sw-border-0"
          onClick={() => removeMetric(metric)}
        />
      )}
    </StyledLegendItem>
  );
}

interface GraphPillsProps {
  isActionable: boolean;
}

const StyledLegendItem = styled.div<GraphPillsProps>`
  display: flex;
  align-items: center;
  border: ${(props) =>
    props.isActionable ? themeBorder('default', 'buttonSecondaryBorder') : 'none'};
`;
