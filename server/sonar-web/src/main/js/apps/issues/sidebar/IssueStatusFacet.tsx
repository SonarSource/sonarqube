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

import { isEqual, sortBy, without } from 'lodash';
import { useIntl } from 'react-intl';
import { FacetBox, FacetItem } from '~design-system';
import { DEFAULT_ISSUES_QUERY } from '../../../components/shared/utils';
import { ISSUE_STATUSES } from '../../../helpers/constants';
import { DocLink } from '../../../helpers/doc-links';
import { IssueStatus } from '../../../types/issues';
import { formatFacetStat } from '../utils';
import { FacetHelp } from './FacetHelp';
import { FacetItemsList } from './FacetItemsList';
import { MultipleSelectionHint } from './MultipleSelectionHint';
import { CommonProps } from './SimpleListStyleFacet';

interface Props extends CommonProps {
  issueStatuses: Array<IssueStatus>;
}

const property = 'issueStatuses';
const headerId = `facet_${property}`;

const defaultStatuses = DEFAULT_ISSUES_QUERY.issueStatuses.split(',') as IssueStatus[];

export function IssueStatusFacet(props: Readonly<Props>) {
  const { issueStatuses = [], stats = {}, fetching, open, help, needIssueSync } = props;
  const intl = useIntl();

  const nbSelectableItems = ISSUE_STATUSES.filter(
    (item) => !defaultStatuses.includes(item) && stats[item],
  ).length;
  const hasDefaultSelection = isEqual(sortBy(issueStatuses), sortBy(defaultStatuses));
  const nbSelectedItems = hasDefaultSelection ? 0 : issueStatuses.length;

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
      help={help ?? <FacetHelp property="issueStatuses" link={DocLink.IssueStatuses} />}
    >
      <FacetItemsList labelledby={headerId}>
        {ISSUE_STATUSES.map((item) => {
          const active = issueStatuses.includes(item);
          const stat = stats[item];

          return (
            <FacetItem
              active={active}
              className="it__search-navigator-facet"
              key={item}
              name={intl.formatMessage({ id: `issue.issue_status.${item}` })}
              onClick={(itemValue: IssueStatus, multiple) => {
                if (multiple) {
                  props.onChange({
                    [property]: active
                      ? without(issueStatuses, itemValue)
                      : [...issueStatuses, itemValue],
                  });
                } else {
                  props.onChange({
                    [property]: active && issueStatuses.length === 1 ? [] : [itemValue],
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
        nbSelectedItems={issueStatuses.length}
      />
    </FacetBox>
  );
}
