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
import DrilldownLink from '../../../components/shared/DrilldownLink';
import { translate } from '../../../helpers/l10n';
import { formatMeasure, findMeasure } from '../../../helpers/measures';
import { getLeakValue } from '../../../components/measure/utils';
import { MEASUREMENTS_MAP, MeasurementType } from '../utils';

interface Props {
  branchLike?: T.BranchLike;
  className?: string;
  component: T.Component;
  measures: T.Measure[];
  type: MeasurementType;
}

export default class MeasurementLabel extends React.Component<Props> {
  getLabelText = () => {
    const { branchLike, component, measures, type } = this.props;
    const { expandedLabelKey, linesMetric, labelKey } = MEASUREMENTS_MAP[type];

    const measure = findMeasure(measures, linesMetric);
    if (!measure) {
      return translate(labelKey);
    } else {
      return (
        <FormattedMessage
          defaultMessage={translate(expandedLabelKey)}
          id={expandedLabelKey}
          values={{
            count: (
              <DrilldownLink branchLike={branchLike} component={component.key} metric={linesMetric}>
                {formatMeasure(getLeakValue(measure), 'SHORT_INT')}
              </DrilldownLink>
            )
          }}
        />
      );
    }
  };

  render() {
    const { branchLike, className, component, measures, type } = this.props;
    const { iconClass, metric } = MEASUREMENTS_MAP[type];

    const measure = findMeasure(measures, metric);

    let value;
    if (measure) {
      value = getLeakValue(measure);
    }

    return (
      <>
        {value === undefined ? (
          <span>—</span>
        ) : (
          <>
            <span className="big-spacer-right">
              {React.createElement(iconClass, { size: 'big', value: Number(value) })}
            </span>
            <DrilldownLink
              branchLike={branchLike}
              className={className}
              component={component.key}
              metric={metric}>
              {formatMeasure(value, 'PERCENT')}
            </DrilldownLink>
          </>
        )}
        <span className="big-spacer-left">{this.getLabelText()}</span>
      </>
    );
  }
}
