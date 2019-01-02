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
import * as classNames from 'classnames';
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import { getLeakValue } from '../measure/utils';
import CoverageRating from '../ui/CoverageRating';
import { formatMeasure } from '../../helpers/measures';
import HelpTooltip from '../controls/HelpTooltip';
import { translate } from '../../helpers/l10n';
import DuplicationsRating from '../ui/DuplicationsRating';
import { getComponentDrilldownUrl } from '../../helpers/urls';

interface Props {
  branchLike: T.BranchLike;
  componentKey: string;
  measures: T.Measure[];
}

export default function BranchMeasures({ branchLike, componentKey, measures }: Props) {
  const newCoverage = measures.find(measure => measure.metric === 'new_coverage');
  const newDuplications = measures.find(
    measure => measure.metric === 'new_duplicated_lines_density'
  );
  if (!newCoverage && !newDuplications) {
    return null;
  }

  return (
    <div className="display-inline-flex-center">
      <BranchCoverage branchLike={branchLike} componentKey={componentKey} measure={newCoverage} />
      <BranchDuplications
        branchLike={branchLike}
        className="big-spacer-left"
        componentKey={componentKey}
        measure={newDuplications}
      />
    </div>
  );
}

interface MeasureProps {
  branchLike: T.BranchLike;
  className?: string;
  componentKey: string;
  measure: T.Measure | undefined;
}

export function BranchCoverage({ branchLike, className, componentKey, measure }: MeasureProps) {
  const value = getLeakValue(measure);
  return (
    <div className={classNames('display-inline-flex-center', className)}>
      <CoverageRating size="small" value={value} />
      <span className="little-spacer-left">
        {value !== undefined ? formatMeasure(value, 'PERCENT') : '–'}
      </span>
      <HelpTooltip
        className="little-spacer-left"
        overlay={
          measure ? (
            <FormattedMessage
              defaultMessage={translate('branches.measures.new_coverage.help')}
              id="branches.measures.new_coverage.help"
              values={{
                link: (
                  <Link
                    to={getComponentDrilldownUrl({
                      componentKey,
                      branchLike,
                      metric: 'new_coverage'
                    })}>
                    {translate('layout.measures')}
                  </Link>
                )
              }}
            />
          ) : (
            translate('branches.measures.new_coverage.missing')
          )
        }
      />
    </div>
  );
}

export function BranchDuplications({ branchLike, className, componentKey, measure }: MeasureProps) {
  const value = getLeakValue(measure);
  return (
    <div className={classNames('display-inline-flex-center', className)}>
      <DuplicationsRating size="small" value={Number(value)} />
      <span className="little-spacer-left">
        {value !== undefined ? formatMeasure(value, 'PERCENT') : '–'}
      </span>
      <HelpTooltip
        className="little-spacer-left"
        overlay={
          measure ? (
            <FormattedMessage
              defaultMessage={translate('branches.measures.new_duplicated_lines_density.help')}
              id="branches.measures.new_duplicated_lines_density.help"
              values={{
                link: (
                  <Link
                    to={getComponentDrilldownUrl({
                      componentKey,
                      branchLike,
                      metric: 'new_duplicated_lines_density'
                    })}>
                    {translate('layout.measures')}
                  </Link>
                )
              }}
            />
          ) : (
            translate('branches.measures.new_duplicated_lines_density.missing')
          )
        }
      />
    </div>
  );
}
