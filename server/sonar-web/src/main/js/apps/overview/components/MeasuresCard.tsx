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
import { Badge, Card, ContentLink, Tooltip, themeBorder, themeColor } from 'design-system';
import * as React from 'react';
import { To } from 'react-router-dom';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { localizeMetric } from '../../../helpers/measures';
import { MetricKey } from '../../../types/metrics';

export interface MeasuresCardProps {
  url: To;
  value?: string;
  metric: MetricKey;
  label: string;
  failed?: boolean;
  icon?: React.ReactElement;
  disabled?: boolean;
  tooltip?: React.ReactNode | null;
}

export default function MeasuresCard(
  props: React.PropsWithChildren<MeasuresCardProps & React.HTMLAttributes<HTMLDivElement>>,
) {
  const { failed, children, metric, icon, value, url, label, disabled, tooltip, ...rest } = props;

  return (
    <StyledCard className="sw-h-fit sw-p-6 sw-rounded-2 sw-text-base" {...rest}>
      <ColorBold className="sw-body-sm-highlight">{translate(label)}</ColorBold>
      {failed && (
        <Badge className="sw-mt-1/2 sw-px-1 sw-ml-2" variant="deleted">
          {translate('overview.measures.failed_badge')}
        </Badge>
      )}
      <div className="sw-flex sw-items-center sw-mt-1 sw-justify-between sw-font-semibold">
        {value ? (
          <Tooltip overlay={tooltip}>
            <ContentLink
              aria-label={translateWithParameters(
                'overview.see_more_details_on_x_of_y',
                value,
                localizeMetric(metric),
              )}
              className="it__overview-measures-value sw-text-lg"
              to={url}
              disabled={disabled}
            >
              {value}
            </ContentLink>
          </Tooltip>
        ) : (
          <ColorBold> — </ColorBold>
        )}
        {icon}
      </div>
      {children && <div className="sw-flex sw-flex-col">{children}</div>}
    </StyledCard>
  );
}

const StyledCard = styled(Card)`
  border: ${themeBorder('default')};
`;

const ColorBold = styled.span`
  color: ${themeColor('pageTitle')};
`;
