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
import { ContentCell, Link, Note, NumericalCell, TableRow } from 'design-system';
import * as React from 'react';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { translateWithParameters } from '../../../helpers/l10n';
import { isDefined } from '../../../helpers/types';
import { getRulesUrl } from '../../../helpers/urls';
import { MetricType } from '../../../types/metrics';
import { RulesFacetName } from '../../../types/rules';

interface Props {
  title: string;
  className?: string;
  count: number | null;
  qprofile: string;
  total: number | null;
  propertyName:
    | RulesFacetName.CleanCodeAttributeCategories
    | RulesFacetName.ImpactSoftwareQualities;
  propertyValue: string;
}

export default function ProfileRulesRow(props: Readonly<Props>) {
  const activeRulesUrl = getRulesUrl({
    qprofile: props.qprofile,
    activation: 'true',
    [props.propertyName]: props.propertyValue,
  });
  const inactiveRulesUrl = getRulesUrl({
    qprofile: props.qprofile,
    activation: 'false',
    [props.propertyName]: props.propertyValue,
  });
  let inactiveCount = null;
  if (props.count != null && props.total != null) {
    inactiveCount = props.total - props.count;
  }

  return (
    <TableRow className={props.className}>
      <ContentCell className="sw-pl-4">{props.title}</ContentCell>
      <NumericalCell>
        {isDefined(props.count) && props.count > 0 ? (
          <Link
            aria-label={translateWithParameters(
              'quality_profile.rules.see_x_active_x_rules',
              props.count,
              props.title,
            )}
            to={activeRulesUrl}
          >
            {formatMeasure(props.count, MetricType.ShortInteger)}
          </Link>
        ) : (
          <Note>0</Note>
        )}
      </NumericalCell>
      <NumericalCell className="sw-pr-4">
        {isDefined(inactiveCount) && inactiveCount > 0 ? (
          <Link
            aria-label={translateWithParameters(
              'quality_profile.rules.see_x_inactive_x_rules',
              inactiveCount,
              props.title,
            )}
            to={inactiveRulesUrl}
          >
            {formatMeasure(inactiveCount, MetricType.ShortInteger)}
          </Link>
        ) : (
          <Note>0</Note>
        )}
      </NumericalCell>
    </TableRow>
  );
}
