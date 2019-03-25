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
import Effort from './Effort';
import MeasuresButtonLink from './MeasuresButtonLink';
import HistoryButtonLink from './HistoryButtonLink';
import MainRating from './MainRating';
import RatingFreshness from './RatingFreshness';
import { translate } from '../../../helpers/l10n';

interface Props {
  component: string;
  measures: T.Dict<string | undefined>;
}

export default function ReliabilityBox({ component, measures }: Props) {
  const rating = measures['reliability_rating'];
  const lastReliabilityChange = measures['last_change_on_reliability_rating'];
  const rawEffort = measures['reliability_rating_effort'];
  const effort = rawEffort ? JSON.parse(rawEffort) : undefined;

  return (
    <div className="portfolio-box portfolio-reliability">
      <h2 className="portfolio-box-title">
        {translate('metric_domain.Reliability')}
        <MeasuresButtonLink component={component} metric="Reliability" />
        <HistoryButtonLink component={component} metric="reliability_rating" />
      </h2>

      {rating && <MainRating component={component} metric="reliability_rating" value={rating} />}

      <RatingFreshness lastChange={lastReliabilityChange} rating={rating} />

      {effort && <Effort component={component} effort={effort} metricKey="reliability_rating" />}
    </div>
  );
}
