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
import styled from '@emotion/styled';
import classNames from 'classnames';
import { Card, ContentLink, PageContentFontWrapper, themeColor } from 'design-system';
import * as React from 'react';
import { To } from 'react-router-dom';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { localizeMetric } from '../../../helpers/measures';
import { MetricKey } from '../../../types/metrics';

interface Props {
  url: To;
  value: string;
  metric: MetricKey;
  label: string;
  failed?: boolean;
  icon?: React.ReactElement;
}

export default function MeasuresCard(
  props: React.PropsWithChildren<Props & React.HTMLAttributes<HTMLDivElement>>,
) {
  const { failed, children, metric, icon, value, url, label, ...rest } = props;

  return (
    <StyledCard
      className={classNames(
        'sw-h-fit sw-p-8 sw-rounded-2 sw-flex sw-justify-between sw-items-center sw-text-base',
        {
          failed,
        },
      )}
      {...rest}
    >
      <PageContentFontWrapper className="sw-flex sw-flex-col sw-gap-1 sw-justify-between">
        <div className="sw-flex sw-items-center sw-gap-2 sw-font-semibold">
          {value ? (
            <ContentLink
              aria-label={translateWithParameters(
                'overview.see_more_details_on_x_of_y',
                value,
                localizeMetric(metric),
              )}
              className="it__overview-measures-value sw-text-lg"
              to={url}
            >
              {value}
            </ContentLink>
          ) : (
            <StyledNoValue> — </StyledNoValue>
          )}
          {translate(label)}
        </div>
        {children && <div className="sw-flex sw-flex-col">{children}</div>}
      </PageContentFontWrapper>

      {icon && <div>{icon}</div>}
    </StyledCard>
  );
}

const StyledNoValue = styled.span`
  color: ${themeColor('pageTitle')};
`;

export const StyledCard = styled(Card)`
  &.failed {
    border-color: ${themeColor('qgCardFailed')};
  }
`;
