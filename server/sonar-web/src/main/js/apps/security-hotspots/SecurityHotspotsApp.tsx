/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { Location } from 'history';
import { flatMap, range } from 'lodash';
import * as React from 'react';
import { addNoFooterPageClass, removeNoFooterPageClass } from 'sonar-ui-common/helpers/pages';
import { getMeasures } from '../../api/measures';
import { getSecurityHotspotList, getSecurityHotspots } from '../../api/security-hotspots';
import { withCurrentUser } from '../../components/hoc/withCurrentUser';
import { Router } from '../../components/hoc/withRouter';
import { getLeakValue } from '../../components/measure/utils';
import { getBranchLikeQuery, isPullRequest, isSameBranchLike } from '../../helpers/branch-like';
import { getStandards } from '../../helpers/security-standard';
import { isLoggedIn } from '../../helpers/users';
import { BranchLike } from '../../types/branch-like';
import {
  HotspotFilters,
  HotspotResolution,
  HotspotStatus,
  HotspotStatusFilter,
  RawHotspot
} from '../../types/security-hotspots';
import SecurityHotspotsAppRenderer from './SecurityHotspotsAppRenderer';
import './styles.css';

const PAGE_SIZE = 500;

interface Props {
  branchLike?: BranchLike;
  currentUser: T.CurrentUser;
  component: T.Component;
  location: Location;
  router: Router;
}

interface State {
  hotspotKeys?: string[];
  hotspots: RawHotspot[];
  hotspotsPageIndex: number;
  hotspotsReviewedMeasure?: string;
  hotspotsTotal?: number;
  loading: boolean;
  loadingMeasure: boolean;
  loadingMore: boolean;
  securityCategories: T.StandardSecurityCategories;
  selectedHotspot: RawHotspot | undefined;
  filters: HotspotFilters;
}

export class SecurityHotspotsApp extends React.PureComponent<Props, State> {
  mounted = false;
  state: State;

  constructor(props: Props) {
    super(props);

    this.state = {
      loading: true,
      loadingMeasure: false,
      loadingMore: false,
      hotspots: [],
      hotspotsPageIndex: 1,
      securityCategories: {},
      selectedHotspot: undefined,
      filters: {
        ...this.constructFiltersFromProps(props),
        status: HotspotStatusFilter.TO_REVIEW
      }
    };
  }

  componentDidMount() {
    this.mounted = true;
    addNoFooterPageClass();
    this.fetchInitialData();
  }

  componentDidUpdate(previous: Props) {
    if (
      this.props.component.key !== previous.component.key ||
      this.props.location.query.hotspots !== previous.location.query.hotspots
    ) {
      this.fetchInitialData();
    }

    if (
      !isSameBranchLike(this.props.branchLike, previous.branchLike) ||
      isLoggedIn(this.props.currentUser) !== isLoggedIn(previous.currentUser) ||
      this.props.location.query.assignedToMe !== previous.location.query.assignedToMe ||
      this.props.location.query.sinceLeakPeriod !== previous.location.query.sinceLeakPeriod
    ) {
      this.setState(({ filters }) => ({
        filters: { ...this.constructFiltersFromProps, ...filters }
      }));
    }
  }

  componentWillUnmount() {
    removeNoFooterPageClass();
    this.mounted = false;
  }

  constructFiltersFromProps(
    props: Props
  ): Pick<HotspotFilters, 'assignedToMe' | 'sinceLeakPeriod'> {
    return {
      assignedToMe: props.location.query.assignedToMe === 'true' && isLoggedIn(props.currentUser),
      sinceLeakPeriod:
        isPullRequest(props.branchLike) || props.location.query.sinceLeakPeriod === 'true'
    };
  }

  handleCallFailure = () => {
    if (this.mounted) {
      this.setState({ loading: false, loadingMore: false });
    }
  };

  fetchInitialData() {
    return Promise.all([
      getStandards(),
      this.fetchSecurityHotspots(),
      this.fetchSecurityHotspotsReviewed()
    ])
      .then(([{ sonarsourceSecurity }, { hotspots, paging }]) => {
        if (!this.mounted) {
          return;
        }

        this.setState({
          hotspots,
          hotspotsTotal: paging.total,
          loading: false,
          securityCategories: sonarsourceSecurity,
          selectedHotspot: hotspots.length > 0 ? hotspots[0] : undefined
        });
      })
      .catch(this.handleCallFailure);
  }

  fetchSecurityHotspotsReviewed = () => {
    const { branchLike, component } = this.props;
    const { filters } = this.state;

    const reviewedHotspotsMetricKey = filters.sinceLeakPeriod
      ? 'new_security_hotspots_reviewed'
      : 'security_hotspots_reviewed';

    this.setState({ loadingMeasure: true });
    return getMeasures({
      component: component.key,
      metricKeys: reviewedHotspotsMetricKey,
      ...getBranchLikeQuery(branchLike)
    })
      .then(measures => {
        if (!this.mounted) {
          return;
        }
        const measure = measures && measures.length > 0 ? measures[0] : undefined;
        const hotspotsReviewedMeasure = filters.sinceLeakPeriod
          ? getLeakValue(measure)
          : measure?.value;

        this.setState({ hotspotsReviewedMeasure, loadingMeasure: false });
      })
      .catch(() => {
        if (this.mounted) {
          this.setState({ loadingMeasure: false });
        }
      });
  };

  fetchSecurityHotspots(page = 1) {
    const { branchLike, component, location } = this.props;
    const { filters } = this.state;

    const hotspotKeys = location.query.hotspots
      ? (location.query.hotspots as string).split(',')
      : undefined;

    this.setState({ hotspotKeys });

    if (hotspotKeys && hotspotKeys.length > 0) {
      return getSecurityHotspotList(hotspotKeys, {
        projectKey: component.key,
        ...getBranchLikeQuery(branchLike)
      });
    }

    const status =
      filters.status === HotspotStatusFilter.TO_REVIEW
        ? HotspotStatus.TO_REVIEW
        : HotspotStatus.REVIEWED;

    const resolution =
      filters.status === HotspotStatusFilter.TO_REVIEW
        ? undefined
        : HotspotResolution[filters.status];

    return getSecurityHotspots({
      projectKey: component.key,
      p: page,
      ps: PAGE_SIZE,
      status,
      resolution,
      onlyMine: filters.assignedToMe,
      sinceLeakPeriod: filters.sinceLeakPeriod,
      ...getBranchLikeQuery(branchLike)
    });
  }

  reloadSecurityHotspotList = () => {
    this.setState({ loading: true });

    return this.fetchSecurityHotspots()
      .then(({ hotspots, paging }) => {
        if (!this.mounted) {
          return;
        }

        this.setState({
          hotspots,
          hotspotsPageIndex: 1,
          hotspotsTotal: paging.total,
          loading: false,
          selectedHotspot: hotspots.length > 0 ? hotspots[0] : undefined
        });
      })
      .catch(this.handleCallFailure);
  };

  handleChangeFilters = (changes: Partial<HotspotFilters>) => {
    this.setState(
      ({ filters }) => ({ filters: { ...filters, ...changes } }),
      () => {
        this.reloadSecurityHotspotList();
        if (changes.sinceLeakPeriod !== undefined) {
          this.fetchSecurityHotspotsReviewed();
        }
      }
    );
  };

  handleHotspotClick = (selectedHotspot: RawHotspot) => this.setState({ selectedHotspot });

  handleHotspotUpdate = (hotspotKey: string) => {
    const { hotspots, hotspotsPageIndex } = this.state;
    const index = hotspots.findIndex(h => h.key === hotspotKey);

    return Promise.all(
      range(hotspotsPageIndex).map(p => this.fetchSecurityHotspots(p + 1 /* pages are 1-indexed */))
    )
      .then(hotspotPages => {
        const allHotspots = flatMap(hotspotPages, 'hotspots');

        const { paging } = hotspotPages[hotspotPages.length - 1];

        const nextHotspot = allHotspots[Math.min(index, allHotspots.length - 1)];

        this.setState({
          hotspots: allHotspots,
          hotspotsPageIndex: paging.pageIndex,
          hotspotsTotal: paging.total,
          selectedHotspot: nextHotspot
        });
      })
      .then(this.fetchSecurityHotspotsReviewed);
  };

  handleShowAllHotspots = () => {
    this.props.router.push({
      ...this.props.location,
      query: { ...this.props.location.query, hotspots: undefined }
    });
  };

  handleLoadMore = () => {
    const { hotspots, hotspotsPageIndex: hotspotPages } = this.state;

    this.setState({ loadingMore: true });

    return this.fetchSecurityHotspots(hotspotPages + 1)
      .then(({ hotspots: additionalHotspots }) => {
        if (!this.mounted) {
          return;
        }

        this.setState({
          hotspots: [...hotspots, ...additionalHotspots],
          hotspotsPageIndex: hotspotPages + 1,
          loadingMore: false
        });
      })
      .catch(this.handleCallFailure);
  };

  render() {
    const { branchLike, component } = this.props;
    const {
      hotspotKeys,
      hotspots,
      hotspotsReviewedMeasure,
      hotspotsTotal,
      loading,
      loadingMeasure,
      loadingMore,
      securityCategories,
      selectedHotspot,
      filters
    } = this.state;

    return (
      <SecurityHotspotsAppRenderer
        branchLike={branchLike}
        component={component}
        filters={filters}
        hotspots={hotspots}
        hotspotsReviewedMeasure={hotspotsReviewedMeasure}
        hotspotsTotal={hotspotsTotal}
        isStaticListOfHotspots={Boolean(hotspotKeys && hotspotKeys.length > 0)}
        loading={loading}
        loadingMeasure={loadingMeasure}
        loadingMore={loadingMore}
        onChangeFilters={this.handleChangeFilters}
        onHotspotClick={this.handleHotspotClick}
        onLoadMore={this.handleLoadMore}
        onShowAllHotspots={this.handleShowAllHotspots}
        onUpdateHotspot={this.handleHotspotUpdate}
        securityCategories={securityCategories}
        selectedHotspot={selectedHotspot}
      />
    );
  }
}

export default withCurrentUser(SecurityHotspotsApp);
