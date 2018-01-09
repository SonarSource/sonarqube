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
import Effort from './Effort';
import MainRating from './MainRating';
import MeasuresButtonLink from './MeasuresButtonLink';
import HistoryButtonLink from './HistoryButtonLink';
import RatingFreshness from './RatingFreshness';
import { translate } from '../../../helpers/l10n';

interface Props {
  component: string;
  measures: { [key: string]: string | undefined };
}

export default function MaintainabilityBox({ component, measures }: Props) {
  const rating = measures['sqale_rating'];
  const lastMaintainabilityChange = measures['last_change_on_maintainability_rating'];
  const rawEffort = measures['maintainability_rating_effort'];
  const effort = rawEffort ? JSON.parse(rawEffort) : undefined;

  return (
    <div className="portfolio-box portfolio-maintainability">
      <h2 className="portfolio-box-title">
        {translate('metric_domain.Maintainability')}
        <MeasuresButtonLink component={component} metric="Maintainability" />
        <HistoryButtonLink component={component} metric="sqale_rating" />
      </h2>

      {rating && <MainRating component={component} metric={'sqale_rating'} value={rating} />}

      <RatingFreshness lastChange={lastMaintainabilityChange} />

      {effort && <Effort component={component} effort={effort} metricKey={'sqale_rating'} />}
    </div>
  );
}
