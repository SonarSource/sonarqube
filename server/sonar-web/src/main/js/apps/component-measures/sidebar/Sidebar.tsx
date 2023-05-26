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
import { withTheme } from '@emotion/react';
import styled from '@emotion/styled';
import {
  FlagMessage,
  LAYOUT_FOOTER_HEIGHT,
  LAYOUT_GLOBAL_NAV_HEIGHT,
  LAYOUT_PROJECT_NAV_HEIGHT,
  themeBorder,
  themeColor,
} from 'design-system/lib';
import * as React from 'react';
import A11ySkipTarget from '../../../components/a11y/A11ySkipTarget';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import { translate } from '../../../helpers/l10n';
import useFollowScroll from '../../../hooks/useFollowScroll';
import { isPortfolioLike } from '../../../types/component';
import { Dict, MeasureEnhanced } from '../../../types/types';
import { KNOWN_DOMAINS, PROJECT_OVERVEW, Query, groupByDomains } from '../utils';
import DomainFacet from './DomainFacet';
import ProjectOverviewFacet from './ProjectOverviewFacet';

interface Props {
  canBrowseAllChildProjects: boolean;
  measures: MeasureEnhanced[];
  qualifier: string;
  selectedMetric: string;
  showFullMeasures: boolean;
  updateQuery: (query: Partial<Query>) => void;
}

export default function Sidebar(props: Props) {
  const {
    showFullMeasures,
    canBrowseAllChildProjects,
    qualifier,
    updateQuery,
    selectedMetric,
    measures,
  } = props;
  const [openFacets, setOpenFacets] = React.useState(getOpenFacets({}, props));
  const { top: topScroll } = useFollowScroll();

  const handleToggleFacet = React.useCallback(
    (name: string) => {
      setOpenFacets((openFacets) => ({ ...openFacets, [name]: !openFacets[name] }));
    },
    [setOpenFacets]
  );

  const handleChangeMetric = React.useCallback(
    (metric: string) => {
      updateQuery({ metric });
    },
    [updateQuery]
  );

  const distanceFromBottom = topScroll + window.innerHeight - document.body.clientHeight;
  const footerVisibleHeight =
    distanceFromBottom > -LAYOUT_FOOTER_HEIGHT ? LAYOUT_FOOTER_HEIGHT + distanceFromBottom : 0;

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
      {!canBrowseAllChildProjects && isPortfolioLike(qualifier) && (
        <FlagMessage
          ariaLabel={translate('component_measures.not_all_measures_are_shown')}
          className="it__portfolio_warning"
          variant="warning"
        >
          {translate('component_measures.not_all_measures_are_shown')}
          <HelpTooltip
            className="spacer-left"
            overlay={translate('component_measures.not_all_measures_are_shown.help')}
          />
        </FlagMessage>
      )}
      <nav
        className="sw-flex sw-flex-col sw-gap-4 sw-p-4"
        aria-label={translate('component_measures.navigation')}
      >
        <A11ySkipTarget
          anchor="measures_filters"
          label={translate('component_measures.skip_to_navigation')}
          weight={10}
        />
        <ProjectOverviewFacet
          onChange={handleChangeMetric}
          selected={selectedMetric}
          value={PROJECT_OVERVEW}
        />
        {groupByDomains(measures).map((domain) => (
          <DomainFacet
            domain={domain}
            key={domain.name}
            onChange={handleChangeMetric}
            onToggle={handleToggleFacet}
            open={openFacets[domain.name] === true}
            selected={selectedMetric}
            showFullMeasures={showFullMeasures}
          />
        ))}
      </nav>
    </StyledSidebar>
  );
}

function getOpenFacets(openFacets: Dict<boolean>, { measures, selectedMetric }: Props) {
  const newOpenFacets = { ...openFacets };
  const measure = measures.find((measure) => measure.metric.key === selectedMetric);
  if (measure && measure.metric && measure.metric.domain) {
    newOpenFacets[measure.metric.domain] = true;
  } else if (KNOWN_DOMAINS.includes(selectedMetric)) {
    newOpenFacets[selectedMetric] = true;
  }
  return newOpenFacets;
}

const StyledSidebar = withTheme(styled.div`
  box-sizing: border-box;
  margin-top: -2rem;

  background-color: ${themeColor('filterbar')};
  border-right: ${themeBorder('default', 'filterbarBorder')};
`);
