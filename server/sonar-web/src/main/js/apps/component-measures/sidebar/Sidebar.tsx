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
import { withTheme } from '@emotion/react';
import styled from '@emotion/styled';
import {
  BareButton,
  LAYOUT_FOOTER_HEIGHT,
  LAYOUT_GLOBAL_NAV_HEIGHT,
  LAYOUT_PROJECT_NAV_HEIGHT,
  SubnavigationGroup,
  SubnavigationItem,
  themeBorder,
  themeColor,
} from 'design-system';
import * as React from 'react';
import A11ySkipTarget from '../../../components/a11y/A11ySkipTarget';
import { translate } from '../../../helpers/l10n';
import useFollowScroll from '../../../hooks/useFollowScroll';
import { MeasureEnhanced } from '../../../types/types';
import { PROJECT_OVERVEW, Query, isProjectOverview, populateDomainsFromMeasures } from '../utils';
import DomainSubnavigation from './DomainSubnavigation';
import { Domain } from '../../../types/measures';

interface Props {
  measures: MeasureEnhanced[];
  selectedMetric: string;
  showFullMeasures: boolean;
  updateQuery: (query: Partial<Query>) => void;
}

export default function Sidebar(props: Readonly<Props>) {
  const { showFullMeasures, updateQuery, selectedMetric, measures } = props;
  const { top: topScroll, scrolledOnce } = useFollowScroll();
  const domains = populateDomainsFromMeasures(measures);

  const handleChangeMetric = React.useCallback(
    (metric: string) => {
      updateQuery({ metric });
    },
    [updateQuery],
  );

  const handleProjectOverviewClick = () => {
    handleChangeMetric(PROJECT_OVERVEW);
  };

  const distanceFromBottom = topScroll + window.innerHeight - document.body.scrollHeight;
  const footerVisibleHeight =
    (scrolledOnce &&
      (distanceFromBottom > -LAYOUT_FOOTER_HEIGHT
        ? LAYOUT_FOOTER_HEIGHT + distanceFromBottom
        : 0)) ||
    0;

  return (
    <StyledSidebar
      className="sw-col-span-3"
      style={{
        top: `${LAYOUT_GLOBAL_NAV_HEIGHT + LAYOUT_PROJECT_NAV_HEIGHT}px`,
        height: `calc(
            100vh - ${LAYOUT_GLOBAL_NAV_HEIGHT + LAYOUT_PROJECT_NAV_HEIGHT + footerVisibleHeight}px
          )`,
      }}
    >
      <section
        className="sw-flex sw-flex-col sw-gap-4 sw-p-4"
        aria-label={translate('component_measures.navigation')}
      >
        <A11ySkipTarget
          anchor="measures_filters"
          label={translate('component_measures.skip_to_navigation')}
          weight={10}
        />
        <SubnavigationGroup>
          <SubnavigationItem
            active={isProjectOverview(selectedMetric)}
            onClick={handleProjectOverviewClick}
          >
            <BareButton aria-current={isProjectOverview(selectedMetric)}>
              {translate('component_measures.overview', PROJECT_OVERVEW, 'subnavigation')}
            </BareButton>
          </SubnavigationItem>
        </SubnavigationGroup>

        {domains.map((domain: Domain) => (
          <DomainSubnavigation
            domain={domain}
            key={domain.name}
            onChange={handleChangeMetric}
            open={isDomainSelected(selectedMetric, domain)}
            selected={selectedMetric}
            showFullMeasures={showFullMeasures}
          />
        ))}
      </section>
    </StyledSidebar>
  );
}

function isDomainSelected(selectedMetric: string, domain: Domain) {
  return (
    selectedMetric === domain.name ||
    domain.measures.some((measure) => measure.metric.key === selectedMetric)
  );
}

const StyledSidebar = withTheme(styled.div`
  box-sizing: border-box;
  margin-top: -2rem;

  background-color: ${themeColor('filterbar')};
  border-right: ${themeBorder('default', 'filterbarBorder')};
  position: sticky;
  overflow-x: hidden;
`);
