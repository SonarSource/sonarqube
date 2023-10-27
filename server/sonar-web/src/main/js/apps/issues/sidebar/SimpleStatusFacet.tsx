/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import { FacetBox, FacetItem } from 'design-system';
import { FacetItemsList } from './FacetItemsList';

import { isEqual, sortBy, without } from 'lodash';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { DEFAULT_ISSUES_QUERY } from '../../../components/shared/utils';
import { SIMPLE_STATUSES } from '../../../helpers/constants';
import { IssueSimpleStatus } from '../../../types/issues';
import { formatFacetStat } from '../utils';
import { MultipleSelectionHint } from './MultipleSelectionHint';
import { CommonProps } from './SimpleListStyleFacet';

interface Props extends CommonProps {
  simpleStatuses: Array<IssueSimpleStatus>;
}

const property = 'simpleStatuses';
const headerId = `facet_${property}`;

const defaultStatuses = DEFAULT_ISSUES_QUERY.simpleStatuses.split(',') as IssueSimpleStatus[];

export function SimpleStatusFacet(props: Readonly<Props>) {
  const { simpleStatuses = [], stats = {}, fetching, open, help, needIssueSync } = props;
  const intl = useIntl();

  const nbSelectableItems = SIMPLE_STATUSES.filter(
    (item) => !defaultStatuses.includes(item) && stats[item],
  ).length;
  const hasDefaultSelection = isEqual(sortBy(simpleStatuses), sortBy(defaultStatuses));
  const nbSelectedItems = hasDefaultSelection ? 0 : simpleStatuses.length;

  return (
    <FacetBox
      className="it__search-navigator-facet-box it__search-navigator-facet-header"
      clearIconLabel={intl.formatMessage({ id: 'clear' })}
      count={nbSelectedItems}
      countLabel={intl.formatMessage({ id: 'x_selected' }, { '0': nbSelectedItems })}
      data-property={property}
      id={headerId}
      loading={fetching}
      name={intl.formatMessage({ id: `issues.facet.${property}` })}
      onClear={() =>
        props.onChange({
          [property]: defaultStatuses,
        })
      }
      onClick={() => props.onToggle(property)}
      open={open}
      help={help}
    >
      <FacetItemsList labelledby={headerId}>
        {SIMPLE_STATUSES.map((item) => {
          const active = simpleStatuses.includes(item);
          const stat = stats[item];

          return (
            <FacetItem
              active={active}
              className="it__search-navigator-facet"
              key={item}
              name={intl.formatMessage({ id: `issue.simple_status.${item}` })}
              onClick={(itemValue: IssueSimpleStatus, multiple) => {
                if (multiple) {
                  props.onChange({
                    [property]: active
                      ? without(simpleStatuses, itemValue)
                      : [...simpleStatuses, itemValue],
                  });
                } else {
                  props.onChange({
                    [property]: active && simpleStatuses.length === 1 ? [] : [itemValue],
                  });
                }
              }}
              stat={(!needIssueSync && formatFacetStat(stat)) ?? 0}
              value={item}
            />
          );
        })}
      </FacetItemsList>

      <MultipleSelectionHint
        nbSelectableItems={nbSelectableItems}
        nbSelectedItems={simpleStatuses.length}
      />
    </FacetBox>
  );
}
