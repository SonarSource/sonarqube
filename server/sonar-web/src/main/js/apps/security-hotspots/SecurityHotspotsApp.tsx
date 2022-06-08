/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { getMeasures } from '../../api/measures';
import { getSecurityHotspotList, getSecurityHotspots } from '../../api/security-hotspots';
import withBranchStatusActions from '../../app/components/branch-status/withBranchStatusActions';
import withCurrentUserContext from '../../app/components/current-user/withCurrentUserContext';
import { Router } from '../../components/hoc/withRouter';
import { getLeakValue } from '../../components/measure/utils';
import { getBranchLikeQuery, isPullRequest, isSameBranchLike } from '../../helpers/branch-like';
import { KeyboardKeys } from '../../helpers/keycodes';
import { scrollToElement } from '../../helpers/scrolling';
import { getStandards } from '../../helpers/security-standard';
import { BranchLike } from '../../types/branch-like';
import { SecurityStandard, Standards } from '../../types/security';
import {
  HotspotFilters,
  HotspotResolution,
  HotspotStatus,
  HotspotStatusFilter,
  RawHotspot
} from '../../types/security-hotspots';
import { Component, Dict } from '../../types/types';
import { CurrentUser, isLoggedIn } from '../../types/users';
import SecurityHotspotsAppRenderer from './SecurityHotspotsAppRenderer';
import './styles.css';
import { getLocations, SECURITY_STANDARDS } from './utils';

const PAGE_SIZE = 500;
interface DispatchProps {
  fetchBranchStatus: (branchLike: BranchLike, projectKey: string) => void;
}

interface OwnProps {
  branchLike?: BranchLike;
  currentUser: CurrentUser;
  component: Component;
  location: Location;
  router: Router;
}

type Props = DispatchProps & OwnProps;

interface State {
  filterByCategory?: { standard: SecurityStandard; category: string };
  filterByCWE?: string;
  filterByFile?: string;
  filters: HotspotFilters;
  hotspotKeys?: string[];
  hotspots: RawHotspot[];
  hotspotsPageIndex: number;
  hotspotsReviewedMeasure?: string;
  hotspotsTotal: number;
  loading: boolean;
  loadingMeasure: boolean;
  loadingMore: boolean;
  selectedHotspot?: RawHotspot;
  selectedHotspotLocationIndex?: number;
  standards: Standards;
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
      hotspotsTotal: 0,
      hotspotsPageIndex: 1,
      selectedHotspot: undefined,
      standards: {
        [SecurityStandard.OWASP_TOP10]: {},
        [SecurityStandard.OWASP_TOP10_2021]: {},
        [SecurityStandard.SANS_TOP25]: {},
        [SecurityStandard.SONARSOURCE]: {},
        [SecurityStandard.CWE]: {}
      },
      filters: {
        ...this.constructFiltersFromProps(props),
        status: HotspotStatusFilter.TO_REVIEW
      }
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchInitialData();
    this.registerKeyboardEvents();
  }

  componentDidUpdate(previous: Props) {
    if (
      this.props.component.key !== previous.component.key ||
      this.props.location.query.hotspots !== previous.location.query.hotspots ||
      SECURITY_STANDARDS.some(s => this.props.location.query[s] !== previous.location.query[s]) ||
      this.props.location.query.file !== previous.location.query.file
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
    this.unregisterKeyboardEvents();
    this.mounted = false;
  }

  registerKeyboardEvents() {
    document.addEventListener('keydown', this.handleKeyDown);
  }

  handleKeyDown = (event: KeyboardEvent) => {
    if (event.key === KeyboardKeys.Alt) {
      event.preventDefault();
      return;
    }

    switch (event.key) {
      case KeyboardKeys.DownArrow: {
        event.preventDefault();
        if (event.altKey) {
          this.selectNextLocation();
        } else {
          this.selectNeighboringHotspot(+1);
        }
        break;
      }
      case KeyboardKeys.UpArrow: {
        event.preventDefault();
        if (event.altKey) {
          this.selectPreviousLocation();
        } else {
          this.selectNeighboringHotspot(-1);
        }
        break;
      }
    }
  };

  selectNextLocation = () => {
    const { selectedHotspotLocationIndex, selectedHotspot } = this.state;
    if (selectedHotspot === undefined) {
      return;
    }
    const locations = getLocations(selectedHotspot.flows, undefined);
    if (locations.length === 0) {
      return;
    }
    const lastIndex = locations.length - 1;

    let newIndex;
    if (selectedHotspotLocationIndex === undefined) {
      newIndex = 0;
    } else if (selectedHotspotLocationIndex === lastIndex) {
      newIndex = undefined;
    } else {
      newIndex = selectedHotspotLocationIndex + 1;
    }
    this.setState({ selectedHotspotLocationIndex: newIndex });
  };

  selectPreviousLocation = () => {
    const { selectedHotspotLocationIndex } = this.state;

    let newIndex;
    if (selectedHotspotLocationIndex === 0) {
      newIndex = undefined;
    } else if (selectedHotspotLocationIndex !== undefined) {
      newIndex = selectedHotspotLocationIndex - 1;
    }
    this.setState({ selectedHotspotLocationIndex: newIndex });
  };

  selectNeighboringHotspot = (shift: number) => {
    this.setState({ selectedHotspotLocationIndex: undefined });
    this.setState(({ hotspots, selectedHotspot }) => {
      const index = selectedHotspot && hotspots.findIndex(h => h.key === selectedHotspot.key);

      if (index !== undefined && index > -1) {
        const newIndex = Math.max(0, Math.min(hotspots.length - 1, index + shift));
        return {
          selectedHotspot: hotspots[newIndex]
        };
      }

      return { selectedHotspot };
    });
  };

  unregisterKeyboardEvents() {
    document.removeEventListener('keydown', this.handleKeyDown);
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
      .then(([standards, { hotspots, paging }]) => {
        if (!this.mounted) {
          return;
        }

        const selectedHotspot = hotspots.length > 0 ? hotspots[0] : undefined;

        this.setState({
          hotspots,
          hotspotsTotal: paging.total,
          loading: false,
          selectedHotspot,
          standards
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

    const standard = SECURITY_STANDARDS.find(
      stnd => stnd !== SecurityStandard.CWE && location.query[stnd] !== undefined
    );
    const filterByCategory = standard
      ? { standard, category: location.query[standard] }
      : undefined;

    const filterByCWE: string | undefined = location.query.cwe;

    const filterByFile: string | undefined = location.query.file;

    this.setState({ filterByCategory, filterByCWE, filterByFile, hotspotKeys });

    if (hotspotKeys && hotspotKeys.length > 0) {
      return getSecurityHotspotList(hotspotKeys, {
        projectKey: component.key,
        ...getBranchLikeQuery(branchLike)
      });
    }

    if (filterByCategory || filterByCWE || filterByFile) {
      const hotspotFilters: Dict<string> = {};

      if (filterByCategory) {
        hotspotFilters[filterByCategory.standard] = filterByCategory.category;
      }
      if (filterByCWE) {
        hotspotFilters[SecurityStandard.CWE] = filterByCWE;
      }
      if (filterByFile) {
        hotspotFilters.files = filterByFile;
      }

      return getSecurityHotspots({
        ...hotspotFilters,
        projectKey: component.key,
        p: page,
        ps: PAGE_SIZE,
        status: HotspotStatus.TO_REVIEW, // we're only interested in unresolved hotspots
        inNewCodePeriod: filters.sinceLeakPeriod && Boolean(filterByFile), // only add leak period when filtering by file
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
      inNewCodePeriod: filters.sinceLeakPeriod,
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

  handleChangeStatusFilter = (status: HotspotStatusFilter) => {
    this.handleChangeFilters({ status });
  };

  handleHotspotClick = (selectedHotspot: RawHotspot) =>
    this.setState({ selectedHotspot, selectedHotspotLocationIndex: undefined });

  handleHotspotUpdate = (hotspotKey: string) => {
    const { hotspots, hotspotsPageIndex } = this.state;
    const { branchLike, component } = this.props;
    const index = hotspots.findIndex(h => h.key === hotspotKey);

    if (isPullRequest(branchLike)) {
      this.props.fetchBranchStatus(branchLike, component.key);
    }

    return Promise.all(
      range(hotspotsPageIndex).map(p => this.fetchSecurityHotspots(p + 1 /* pages are 1-indexed */))
    )
      .then(hotspotPages => {
        const allHotspots = flatMap(hotspotPages, 'hotspots');

        const { paging } = hotspotPages[hotspotPages.length - 1];

        const nextHotspot = allHotspots[Math.min(index, allHotspots.length - 1)];

        this.setState(({ selectedHotspot }) => ({
          hotspots: allHotspots,
          hotspotsPageIndex: paging.pageIndex,
          hotspotsTotal: paging.total,
          selectedHotspot: selectedHotspot?.key === hotspotKey ? nextHotspot : selectedHotspot
        }));
      })
      .then(this.fetchSecurityHotspotsReviewed);
  };

  handleShowAllHotspots = () => {
    this.props.router.push({
      ...this.props.location,
      query: {
        ...this.props.location.query,
        hotspots: undefined,
        [SecurityStandard.CWE]: undefined,
        [SecurityStandard.OWASP_TOP10]: undefined,
        [SecurityStandard.SANS_TOP25]: undefined,
        [SecurityStandard.SONARSOURCE]: undefined,
        [SecurityStandard.OWASP_TOP10_2021]: undefined,
        file: undefined
      }
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

  handleLocationClick = (locationIndex?: number) => {
    const { selectedHotspotLocationIndex } = this.state;

    if (locationIndex === undefined || locationIndex === selectedHotspotLocationIndex) {
      this.setState({
        selectedHotspotLocationIndex: undefined
      });
    } else {
      this.setState({
        selectedHotspotLocationIndex: locationIndex
      });
    }
  };

  handleScroll = (element: Element, bottomOffset = 100) => {
    const scrollableElement = document.querySelector('.layout-page-side');
    if (element && scrollableElement) {
      scrollToElement(element, { topOffset: 150, bottomOffset, parent: scrollableElement });
    }
  };

  render() {
    const { branchLike, component } = this.props;
    const {
      filterByCategory,
      filterByCWE,
      filterByFile,
      filters,
      hotspotKeys,
      hotspots,
      hotspotsReviewedMeasure,
      hotspotsTotal,
      loading,
      loadingMeasure,
      loadingMore,
      selectedHotspot,
      selectedHotspotLocationIndex,
      standards
    } = this.state;

    return (
      <SecurityHotspotsAppRenderer
        branchLike={branchLike}
        component={component}
        filters={filters}
        filterByCategory={filterByCategory}
        filterByCWE={filterByCWE}
        filterByFile={filterByFile}
        hotspots={hotspots}
        hotspotsReviewedMeasure={hotspotsReviewedMeasure}
        hotspotsTotal={hotspotsTotal}
        isStaticListOfHotspots={Boolean(
          (hotspotKeys && hotspotKeys.length > 0) || filterByCategory || filterByCWE || filterByFile
        )}
        loading={loading}
        loadingMeasure={loadingMeasure}
        loadingMore={loadingMore}
        onChangeFilters={this.handleChangeFilters}
        onHotspotClick={this.handleHotspotClick}
        onLoadMore={this.handleLoadMore}
        onShowAllHotspots={this.handleShowAllHotspots}
        onSwitchStatusFilter={this.handleChangeStatusFilter}
        onUpdateHotspot={this.handleHotspotUpdate}
        onLocationClick={this.handleLocationClick}
        onScroll={this.handleScroll}
        securityCategories={standards[SecurityStandard.SONARSOURCE]}
        selectedHotspot={selectedHotspot}
        selectedHotspotLocation={selectedHotspotLocationIndex}
        standards={standards}
      />
    );
  }
}

export default withCurrentUserContext(withBranchStatusActions(SecurityHotspotsApp));
