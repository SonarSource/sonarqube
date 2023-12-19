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
import { Badge, ContentLink } from 'design-system';
import * as React from 'react';
import { Path } from 'react-router-dom';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { localizeMetric } from '../../../helpers/measures';
import { MetricKey } from '../../../types/metrics';

interface IssueMeasuresCardInnerProps extends React.HTMLAttributes<HTMLDivElement> {
  metric: MetricKey;
  value?: string;
  header: React.ReactNode;
  url: Path;
  failed?: boolean;
  icon?: React.ReactNode;
  footer?: React.ReactNode;
}

export function IssueMeasuresCardInner(props: Readonly<IssueMeasuresCardInnerProps>) {
  const { header, metric, icon, value, url, failed, footer, ...rest } = props;

  return (
    <div className="sw-w-1/3 sw-flex sw-flex-col sw-gap-3" {...rest}>
      <div className="sw-flex sw-flex-col sw-gap-2 sw-font-semibold">
        <div className="sw-flex sw-items-center sw-gap-2">
          {header}

          {failed && (
            <Badge className="sw-h-fit" variant="deleted">
              {translate('overview.measures.failed_badge')}
            </Badge>
          )}
        </div>
        <div className="sw-flex sw-justify-between sw-items-center sw-h-9">
          <div className="sw-h-fit">
            <ContentLink
              aria-label={translateWithParameters(
                'overview.see_more_details_on_x_of_y',
                value ?? '0',
                localizeMetric(metric),
              )}
              className="it__overview-measures-value sw-w-fit sw-text-lg"
              to={url}
            >
              {value ?? '0'}
            </ContentLink>
          </div>

          {icon}
        </div>
      </div>
      {footer}
    </div>
  );
}
