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
import { HotspotRating, HotspotRatingEnum, SubnavigationHeading, themeColor } from 'design-system';
import { groupBy } from 'lodash';
import * as React from 'react';
import ListFooter from '../../../components/controls/ListFooter';
import { translate } from '../../../helpers/l10n';
import { RawHotspot } from '../../../types/security-hotspots';
import { Dict, StandardSecurityCategories } from '../../../types/types';
import { RISK_EXPOSURE_LEVELS, groupByCategory } from '../utils';
import HotspotCategory from './HotspotCategory';

interface Props {
  hotspots: RawHotspot[];
  hotspotsTotal: number;
  loadingMore: boolean;
  onHotspotClick: (hotspot: RawHotspot) => void;
  onLoadMore: () => void;
  onLocationClick: (index?: number) => void;
  securityCategories: StandardSecurityCategories;
  selectedHotspot: RawHotspot;
  selectedHotspotLocation?: number;
}

interface State {
  expandedCategories: Dict<boolean>;
  groupedHotspots: Array<{
    categories: Array<{ hotspots: RawHotspot[]; key: string; title: string }>;
    risk: HotspotRatingEnum;
  }>;
}

export default class HotspotList extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);

    this.state = {
      expandedCategories: { [props.selectedHotspot.securityCategory]: true },
      groupedHotspots: this.groupHotspots(props.hotspots, props.securityCategories),
    };
  }

  componentDidUpdate(prevProps: Props) {
    // Force open the category of selected hotspot
    if (
      this.props.selectedHotspot.securityCategory !== prevProps.selectedHotspot.securityCategory
    ) {
      this.handleToggleCategory(this.props.selectedHotspot.securityCategory, true);
    }

    // Compute the hotspot tree from the list
    if (
      this.props.hotspots !== prevProps.hotspots ||
      this.props.securityCategories !== prevProps.securityCategories
    ) {
      const groupedHotspots = this.groupHotspots(
        this.props.hotspots,
        this.props.securityCategories,
      );
      this.setState({ groupedHotspots });
    }

    if (
      this.props.selectedHotspotLocation !== undefined &&
      this.props.selectedHotspotLocation !== prevProps.selectedHotspotLocation
    ) {
      const { selectedHotspot } = this.props;
      this.handleToggleCategory(selectedHotspot.securityCategory, true);
    }
  }

  groupHotspots = (hotspots: RawHotspot[], securityCategories: StandardSecurityCategories) => {
    const risks = groupBy(hotspots, (h) => h.vulnerabilityProbability);

    return RISK_EXPOSURE_LEVELS.map((risk) => ({
      risk,
      categories: groupByCategory(risks[risk], securityCategories),
    })).filter((risk) => risk.categories.length > 0);
  };

  handleToggleCategory = (categoryKey: string, value: boolean) => {
    this.setState(({ expandedCategories }) => {
      return {
        expandedCategories: {
          ...expandedCategories,
          [categoryKey]: value,
        },
      };
    });
  };

  render() {
    const { hotspots, hotspotsTotal, loadingMore, selectedHotspot, selectedHotspotLocation } =
      this.props;

    const { expandedCategories, groupedHotspots } = this.state;

    return (
      <StyledContainer>
        <div className="sw-mt-8 sw-mb-4">
          {groupedHotspots.map((riskGroup, riskGroupIndex) => {
            const isLastRiskGroup = riskGroupIndex === groupedHotspots.length - 1;

            return (
              <div className="sw-mb-4" key={riskGroup.risk}>
                <SubnavigationHeading as="h2" className="sw-px-0">
                  <span className="sw-flex sw-items-center">
                    <span className="sw-body-sm-highlight">
                      {translate('hotspots.risk_exposure')}:
                    </span>
                    <HotspotRating className="sw-ml-2 sw-mr-1" rating={riskGroup.risk} />
                    {translate('risk_exposure', riskGroup.risk)}
                  </span>
                </SubnavigationHeading>
                <div>
                  {riskGroup.categories.map((category, categoryIndex) => {
                    const isLastCategory = categoryIndex === riskGroup.categories.length - 1;

                    return (
                      <div className="sw-mb-2" key={category.key}>
                        <HotspotCategory
                          expanded={Boolean(expandedCategories[category.key])}
                          onSetExpanded={this.handleToggleCategory.bind(this, category.key)}
                          hotspots={category.hotspots}
                          isLastAndIncomplete={
                            isLastRiskGroup && isLastCategory && hotspots.length < hotspotsTotal
                          }
                          onHotspotClick={this.props.onHotspotClick}
                          rating={riskGroup.risk}
                          selectedHotspot={selectedHotspot}
                          selectedHotspotLocation={selectedHotspotLocation}
                          onLocationClick={this.props.onLocationClick}
                          title={category.title}
                        />
                      </div>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>
        <ListFooter
          count={hotspots.length}
          loadMore={!loadingMore ? this.props.onLoadMore : undefined}
          loading={loadingMore}
          total={hotspotsTotal}
        />
      </StyledContainer>
    );
  }
}

const StyledContainer = withTheme(styled.div`
  background-color: ${themeColor('subnavigation')};
`);
