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
import { useTheme } from '@emotion/react';
import styled from '@emotion/styled';
import classNames from 'classnames';
import {
  CloseIcon,
  FlagWarningIcon,
  InteractiveIcon,
  Theme,
  themeBorder,
  themeColor,
} from 'design-system';
import * as React from 'react';
import { ChartLegendIcon } from '../../components/icons/ChartLegendIcon';
import { translateWithParameters } from '../../helpers/l10n';

interface Props {
  className?: string;
  index: number;
  metric: string;
  name: string;
  showWarning?: boolean;
  removeMetric?: (metric: string) => void;
}

export function GraphsLegendItem({
  className,
  index,
  metric,
  name,
  removeMetric,
  showWarning,
}: Props) {
  const theme = useTheme() as Theme;

  const isActionable = removeMetric !== undefined;

  return (
    <StyledLegendItem
      className={classNames('sw-px-2 sw-py-1 sw-rounded-2', className)}
      isActionable={isActionable}
    >
      {showWarning ? (
        <FlagWarningIcon className="sw-mr-2" />
      ) : (
        <ChartLegendIcon className="sw-mr-2" index={index} />
      )}
      <span className="sw-body-sm" style={{ color: themeColor('graphCursorLineColor')({ theme }) }}>
        {name}
      </span>
      {isActionable && (
        <InteractiveIcon
          Icon={CloseIcon}
          aria-label={translateWithParameters('project_activity.graphs.custom.remove_metric', name)}
          className="sw-ml-2"
          size="small"
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
