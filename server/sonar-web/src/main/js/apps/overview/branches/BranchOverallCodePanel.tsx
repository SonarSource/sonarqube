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
import * as React from 'react';
import { SoftwareQuality } from '../../../types/clean-code-taxonomy';
import { MetricKey } from '../../../types/metrics';
import { Component, MeasureEnhanced } from '../../../types/types';
import SoftwareImpactMeasureCard from './SoftwareImpactMeasureCard';

export interface BranchOverallCodePanelProps {
  component: Component;
  measures: MeasureEnhanced[];
}

export default function BranchOverallCodePanel(props: Readonly<BranchOverallCodePanelProps>) {
  const { component, measures } = props;

  return (
    <div className="sw-flex sw-gap-4">
      <SoftwareImpactMeasureCard
        component={component}
        softwareQuality={SoftwareQuality.Security}
        ratingMetricKey={MetricKey.security_rating}
        measures={measures}
      />
      <SoftwareImpactMeasureCard
        component={component}
        softwareQuality={SoftwareQuality.Reliability}
        ratingMetricKey={MetricKey.reliability_rating}
        measures={measures}
      />
      <SoftwareImpactMeasureCard
        component={component}
        softwareQuality={SoftwareQuality.Maintainability}
        ratingMetricKey={MetricKey.sqale_rating}
        measures={measures}
      />
    </div>
  );
}
