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

import { ContentCell, Link, Note, NumericalCell, TableRow } from '~design-system';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { MetricType } from '~sonar-aligned/types/metrics';
import { translateWithParameters } from '../../../helpers/l10n';
import { isDefined } from '../../../helpers/types';
import { getRulesUrl } from '../../../helpers/urls';
import { RulesFacetName } from '../../../types/rules';
import { RuleType } from '../../../types/types';

interface Props {
  organization: string;
  className?: string;
  count: number | null;
  propertyName?:
    | RulesFacetName.CleanCodeAttributeCategories
    | RulesFacetName.ImpactSoftwareQualities;
  propertyValue?: string;
  qprofile: string;
  title: string;
  total: number | null;
  type?: RuleType;
}

export default function ProfileRulesRow(props: Readonly<Props>) {
  const { qprofile, count, className, propertyName, propertyValue, title, total, type } = props;

  const typeOrCCTQuery = {
    ...(propertyName ? { [propertyName]: propertyValue } : {}),
    ...(type ? { types: type } : {}),
  };
  const activeRulesUrl = getRulesUrl({
    qprofile,
    activation: 'true',
    ...typeOrCCTQuery,
  }, props.organization);
  const inactiveRulesUrl = getRulesUrl({
    qprofile,
    activation: 'false',
    ...typeOrCCTQuery,
  }, props.organization);
  let inactiveCount = null;
  if (count != null && total != null) {
    inactiveCount = total - count;
  }

  return (
    <TableRow className={className}>
      <ContentCell className="sw-pl-4">{title}</ContentCell>
      <NumericalCell>
        {isDefined(count) && count > 0 ? (
          <Link
            aria-label={translateWithParameters(
              'quality_profile.rules.see_x_active_x_rules',
              count,
              props.title,
            )}
            to={activeRulesUrl}
          >
            {formatMeasure(count, MetricType.ShortInteger)}
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
              title,
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
