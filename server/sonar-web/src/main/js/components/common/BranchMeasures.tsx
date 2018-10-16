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
import { formatMeasure } from '../../helpers/measures';
import HelpTooltip from '../controls/HelpTooltip';
import { translate } from '../../helpers/l10n';
import DuplicationsRating from '../ui/DuplicationsRating';

interface Props {
  measures: Measure[];
}

export default function BranchMeasures({ measures }: Props) {
  const newCoverage = measures.find(measure => measure.metric === 'new_coverage');
  const newDuplications = measures.find(
    measure => measure.metric === 'new_duplicated_lines_density'
  );
  if (!newCoverage && !newDuplications) {
    return null;
  }

  return (
    <div className="display-inline-flex-center">
      {newCoverage && <BranchCoverage measure={newCoverage} />}
      {newDuplications && (
        <BranchDuplications
          className={classNames({ 'big-spacer-left': Boolean(newCoverage) })}
          measure={newDuplications}
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
  const value = getLeakValue(measure);
  return (
    <div className={classNames('display-inline-flex-center', className)}>
      <CoverageRating size="small" value={value} />
      <span className="little-spacer-left">{formatMeasure(value, 'PERCENT')}</span>
      <HelpTooltip
        className="little-spacer-left"
        overlay={translate('branches.measures', measure.metric, 'help')}
      />
    </div>
  );
}

export function BranchDuplications({ className, measure }: MeasureProps) {
  const value = getLeakValue(measure);
  return value !== undefined ? (
    <div className={classNames('display-inline-flex-center', className)}>
      <DuplicationsRating size="small" value={Number(value)} />
      <span className="little-spacer-left">{formatMeasure(value, 'PERCENT')}</span>
      <HelpTooltip
        className="little-spacer-left"
        overlay={translate('branches.measures', measure.metric, 'help')}
      />
    </div>
  ) : null;
}
