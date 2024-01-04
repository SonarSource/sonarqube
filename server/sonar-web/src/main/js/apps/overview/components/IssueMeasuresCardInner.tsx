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
import { Badge, ContentLink, LightLabel, NoDataIcon, themeColor } from 'design-system';
import * as React from 'react';
import { Path } from 'react-router-dom';
import Tooltip from '../../../components/controls/Tooltip';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { localizeMetric } from '../../../helpers/measures';
import { MetricKey } from '../../../types/metrics';
import { OverviewDisabledLinkTooltip } from './OverviewDisabledLinkTooltip';

const NO_DATA_ICON_SIZE = 36;

interface IssueMeasuresCardInnerProps extends React.HTMLAttributes<HTMLDivElement> {
  metric: MetricKey;
  value?: string;
  header: React.ReactNode;
  url: Path;
  failed?: boolean;
  icon?: React.ReactNode;
  linkDisabled?: boolean;
  footer?: React.ReactNode;
  noDataIconClassName?: string;
}

export function IssueMeasuresCardInner(props: Readonly<IssueMeasuresCardInnerProps>) {
  const {
    header,
    metric,
    icon,
    value,
    url,
    failed,
    footer,
    className,
    noDataIconClassName,
    linkDisabled,
    ...rest
  } = props;

  return (
    <div className={classNames('sw-flex sw-flex-col sw-gap-3', className)} {...rest}>
      <div className="sw-flex sw-flex-col sw-gap-2 sw-font-semibold">
        <ColorBold className="sw-flex sw-items-center sw-gap-2 sw-body-sm-highlight">
          {header}

          {failed && (
            <Badge className="sw-h-fit" variant="deleted">
              {translate('overview.measures.failed_badge')}
            </Badge>
          )}
        </ColorBold>
        <div className="sw-flex sw-justify-between sw-items-center sw-h-9">
          <div className="sw-h-fit">
            {value ? (
              <Tooltip
                classNameSpace={linkDisabled ? 'tooltip' : 'sw-hidden'}
                overlay={<OverviewDisabledLinkTooltip />}
              >
                <ContentLink
                  disabled={linkDisabled}
                  aria-label={translateWithParameters(
                    'overview.see_more_details_on_x_of_y',
                    value,
                    localizeMetric(metric),
                  )}
                  className="it__overview-measures-value sw-w-fit sw-text-lg"
                  to={url}
                >
                  {value}
                </ContentLink>
              </Tooltip>
            ) : (
              <Tooltip overlay={translate('no_data')}>
                <LightLabel aria-label={translate('no_data')}> — </LightLabel>
              </Tooltip>
            )}
          </div>
          {value ? (
            icon
          ) : (
            <NoDataIcon
              className={noDataIconClassName}
              width={NO_DATA_ICON_SIZE}
              height={NO_DATA_ICON_SIZE}
            />
          )}
        </div>
      </div>
      {footer}
    </div>
  );
}

const ColorBold = styled.div`
  color: ${themeColor('pageTitle')};
`;
