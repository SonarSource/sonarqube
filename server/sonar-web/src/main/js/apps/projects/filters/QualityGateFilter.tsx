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
import { FacetBox, FacetItem, HelperHintIcon, QualityGateIndicator } from 'design-system';
import { without } from 'lodash';
import * as React from 'react';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import { translate } from '../../../helpers/l10n';
import { isDefined } from '../../../helpers/types';
import { RawQuery, Status } from '../../../types/types';
import { FacetItemsList } from '../../issues/sidebar/FacetItemsList';
import { formatFacetStat } from '../../issues/utils';
import { Facet } from '../types';

export interface Props {
  facet?: Facet;
  maxFacetValue?: number;
  onQueryChange: (change: RawQuery) => void;
  value?: Array<string>;
}

const HEADER_ID = `facet_quality_gate`;

export default function QualityGateFacet(props: Props) {
  const { facet, maxFacetValue, onQueryChange, value } = props;
  const hasWarnStatus = facet?.['WARN'] !== undefined;
  const options = hasWarnStatus ? ['OK', 'WARN', 'ERROR'] : ['OK', 'ERROR'];

  const onItemClick = React.useCallback(
    (itemValue: string, multiple: boolean) => {
      const active = value?.includes(itemValue);

      if (multiple) {
        onQueryChange({
          gate: (active ? without(value, itemValue) : [...(value ?? []), itemValue]).join(','),
        });
      } else {
        onQueryChange({
          gate: (active && value?.length === 1 ? [] : [itemValue]).join(','),
        });
      }
    },
    [onQueryChange, value],
  );

  return (
    <FacetBox id={HEADER_ID} open name={translate('projects.facets.quality_gate')}>
      <FacetItemsList labelledby={HEADER_ID}>
        {options.map((option) => {
          const facetValue = facet?.[option];

          const statBarPercent =
            isDefined(facetValue) && isDefined(maxFacetValue) && maxFacetValue > 0
              ? facetValue / maxFacetValue
              : undefined;

          return (
            <FacetItem
              disableZero={false}
              key={option}
              active={value?.includes(option)}
              name={renderOption(option)}
              onClick={onItemClick}
              value={option}
              stat={formatFacetStat(facet?.[option]) ?? 0}
              statBarPercent={statBarPercent}
            />
          );
        })}
      </FacetItemsList>
    </FacetBox>
  );
}

function renderOption(option: string) {
  return (
    <div className="sw-flex sw-items-center">
      <QualityGateIndicator status={option as Status} size="sm" />
      <span className="sw-ml-1">{translate('metric.level', option)}</span>
      {option === 'WARN' && (
        <HelpTooltip overlay={translate('projects.facets.quality_gate.warning_help')}>
          <HelperHintIcon className="sw-ml-1" />
        </HelpTooltip>
      )}
    </div>
  );
}
