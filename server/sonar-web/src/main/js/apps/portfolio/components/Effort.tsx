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
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import Rating from 'sonar-ui-common/components/ui/Rating';
import { translate } from 'sonar-ui-common/helpers/l10n';
import Measure from '../../../components/measure/Measure';
import { getComponentDrilldownUrl } from '../../../helpers/urls';

interface Props {
  component: string;
  effort: { projects: number; rating: number };
  metricKey: string;
}

export default function Effort({ component, effort, metricKey }: Props) {
  return (
    <div className="portfolio-effort">
      <FormattedMessage
        defaultMessage={translate('portfolio.x_in_y')}
        id="portfolio.x_in_y"
        values={{
          projects: (
            <Link
              to={getComponentDrilldownUrl({
                componentKey: component,
                metric: metricKey,
                listView: true
              })}>
              <span>
                <Measure
                  className="little-spacer-right"
                  metricKey="projects"
                  metricType="SHORT_INT"
                  value={String(effort.projects)}
                />
                {effort.projects === 1
                  ? translate('project_singular')
                  : translate('project_plural')}
              </span>
            </Link>
          ),
          rating: <Rating small={true} value={effort.rating} />
        }}
      />
    </div>
  );
}
