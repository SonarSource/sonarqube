/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
//@flow
import React from 'react';
import QualityGateConditions from './QualityGateConditions';
import EmptyQualityGate from './EmptyQualityGate';
import { translate } from '../../../helpers/l10n';
import Level from '../../../components/ui/Level';
import type { Component, MeasuresList } from '../types';

function parseQualityGateDetails(rawDetails: string) {
  return JSON.parse(rawDetails);
}

function isProject(component: Component) {
  return component.qualifier === 'TRK';
}

type Props = {
  component: Component,
  measures: MeasuresList
};

export default function QualityGate({ component, measures }: Props) {
  const statusMeasure = measures.find(measure => measure.metric.key === 'alert_status');
  const detailsMeasure = measures.find(measure => measure.metric.key === 'quality_gate_details');

  if (!statusMeasure) {
    return isProject(component) ? <EmptyQualityGate /> : null;
  }

  const level = statusMeasure.value;

  let conditions = [];
  if (detailsMeasure && detailsMeasure.value) {
    conditions = parseQualityGateDetails(detailsMeasure.value).conditions;
  }

  return (
    <div className="overview-quality-gate" id="overview-quality-gate">
      <h2 className="overview-title">
        {translate('overview.quality_gate')}
        <Level level={level} />
      </h2>

      {conditions.length > 0 &&
        <QualityGateConditions component={component} conditions={conditions} />}
    </div>
  );
}
