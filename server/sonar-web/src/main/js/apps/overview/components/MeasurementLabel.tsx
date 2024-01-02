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
import { FormattedMessage } from 'react-intl';
import { getLeakValue } from '../../../components/measure/utils';
import DrilldownLink from '../../../components/shared/DrilldownLink';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { findMeasure, formatMeasure, localizeMetric } from '../../../helpers/measures';
import { BranchLike } from '../../../types/branch-like';
import { Component, MeasureEnhanced } from '../../../types/types';
import {
  getMeasurementIconClass,
  getMeasurementLabelKeys,
  getMeasurementLinesMetricKey,
  getMeasurementMetricKey,
  MeasurementType,
} from '../utils';

interface Props {
  branchLike?: BranchLike;
  centered?: boolean;
  component: Component;
  measures: MeasureEnhanced[];
  type: MeasurementType;
  useDiffMetric?: boolean;
}

export default class MeasurementLabel extends React.Component<Props> {
  getLabelText = () => {
    const { branchLike, component, measures, type, useDiffMetric = false } = this.props;
    const { expandedLabelKey, labelKey } = getMeasurementLabelKeys(type, useDiffMetric);
    const linesMetric = getMeasurementLinesMetricKey(type, useDiffMetric);
    const measure = findMeasure(measures, linesMetric);

    if (!measure) {
      return translate(labelKey);
    }

    const value = useDiffMetric ? getLeakValue(measure) : measure.value;

    return (
      <FormattedMessage
        defaultMessage={translate(expandedLabelKey)}
        id={expandedLabelKey}
        values={{
          count: (
            <DrilldownLink
              branchLike={branchLike}
              className="big"
              component={component.key}
              metric={linesMetric}
            >
              {formatMeasure(value, 'SHORT_INT')}
            </DrilldownLink>
          ),
        }}
      />
    );
  };

  render() {
    const { branchLike, centered, component, measures, type, useDiffMetric = false } = this.props;
    const iconClass = getMeasurementIconClass(type);
    const metricKey = getMeasurementMetricKey(type, useDiffMetric);
    const measure = findMeasure(measures, metricKey);

    let value;
    if (measure) {
      value = useDiffMetric ? getLeakValue(measure) : measure.value;
    }

    if (value === undefined) {
      return (
        <div className="display-flex-center">
          <span aria-label={translate('no_data')} className="overview-measures-empty-value" />
          <span className="big-spacer-left">{this.getLabelText()}</span>
        </div>
      );
    }

    const icon = React.createElement(iconClass, { size: 'big', value: Number(value) });
    const formattedValue = formatMeasure(value, 'PERCENT', {
      decimals: 2,
      omitExtraDecimalZeros: true,
    });
    const link = (
      <DrilldownLink
        ariaLabel={translateWithParameters(
          'overview.see_more_details_on_x_of_y',
          formattedValue,
          localizeMetric(metricKey)
        )}
        branchLike={branchLike}
        className="overview-measures-value text-light"
        component={component.key}
        metric={metricKey}
      >
        {formattedValue}
      </DrilldownLink>
    );
    const label = this.getLabelText();

    return centered ? (
      <div className="display-flex-column flex-1">
        <div className="display-flex-center display-flex-justify-center">
          <span className="big-spacer-right">{icon}</span>
          {link}
        </div>
        <div className="spacer-top text-center">{label}</div>
      </div>
    ) : (
      <div className="display-flex-center">
        <span className="big-spacer-right">{icon}</span>
        <div className="display-flex-column">
          <span>{link}</span>
          <span className="spacer-top">{label}</span>
        </div>
      </div>
    );
  }
}
