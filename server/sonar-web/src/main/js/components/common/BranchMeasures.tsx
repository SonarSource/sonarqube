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
import * as classNames from 'classnames';
import { Measure } from '../../app/types';
import { getLeakValue } from '../measure/utils';
import CoverageRating from '../ui/CoverageRating';
import { formatMeasure, isDiffMetric } from '../../helpers/measures';
import HelpTooltip from '../controls/HelpTooltip';
import { translate } from '../../helpers/l10n';

interface Props {
  measures: Measure[];
}

export default function BranchMeasures({ measures }: Props) {
  const coverage = measures.find(measure => measure.metric === 'coverage');
  const newCoverage = measures.find(measure => measure.metric === 'new_coverage');
  if (!coverage && !newCoverage) {
    return null;
  }

  return (
    <div className="display-inline-flex-center">
      {coverage && <BranchCoverage measure={coverage} />}
      {newCoverage && (
        <BranchCoverage
          className={classNames({ 'big-spacer-left': Boolean(coverage) })}
          measure={newCoverage}
        />
      )}
    </div>
  );
}

interface MeasureProps {
  className?: string;
  measure: Measure;
}

export function BranchCoverage({ className, measure }: MeasureProps) {
  const isDiff = isDiffMetric(measure.metric);
  const value = isDiff ? getLeakValue(measure) : measure.value;
  return (
    <div
      className={classNames(
        'display-inline-flex-center',
        { 'rounded leak-box': isDiff },
        className
      )}>
      <CoverageRating size="xs" value={value} />
      <span className="little-spacer-left">{formatMeasure(value, 'PERCENT')}</span>
      <HelpTooltip
        className="little-spacer-left"
        overlay={translate('branches.measures', measure.metric, 'help')}
      />
    </div>
  );
}
