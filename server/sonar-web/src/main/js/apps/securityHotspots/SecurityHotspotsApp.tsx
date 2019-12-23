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
import * as React from 'react';
import { addNoFooterPageClass, removeNoFooterPageClass } from 'sonar-ui-common/helpers/pages';
import { getSecurityHotspotList, getSecurityHotspots } from '../../api/security-hotspots';
import { withCurrentUser } from '../../components/hoc/withCurrentUser';
import { Router } from '../../components/hoc/withRouter';
import { getBranchLikeQuery, isPullRequest, isSameBranchLike } from '../../helpers/branch-like';
import { getStandards } from '../../helpers/security-standard';
import { isLoggedIn } from '../../helpers/users';
import { BranchLike } from '../../types/branch-like';
import {
  HotspotFilters,
  HotspotResolution,
  HotspotStatus,
  HotspotStatusFilter,
  HotspotUpdate,
  RawHotspot
} from '../../types/security-hotspots';
import SecurityHotspotsAppRenderer from './SecurityHotspotsAppRenderer';
import './styles.css';
import { sortHotspots } from './utils';

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
  loading: boolean;
  securityCategories: T.StandardSecurityCategories;
  selectedHotspotKey: string | undefined;
  filters: HotspotFilters;
}

export class SecurityHotspotsApp extends React.PureComponent<Props, State> {
  mounted = false;
  state: State;

  constructor(props: Props) {
    super(props);

    this.state = {
      loading: true,
      hotspots: [],
      securityCategories: {},
      selectedHotspotKey: undefined,
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
      this.props.location.query.newCode !== previous.location.query.newCode
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

  constructFiltersFromProps(props: Props): Pick<HotspotFilters, 'assignedToMe' | 'newCode'> {
    return {
      assignedToMe:
        props.location.query.assignedToMe !== undefined
          ? props.location.query.assignedToMe === 'true'
          : isLoggedIn(props.currentUser),
      newCode: isPullRequest(props.branchLike) || props.location.query.newCode === 'true'
    };
  }

  handleCallFailure = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  fetchInitialData() {
    return Promise.all([getStandards(), this.fetchSecurityHotspots()])
      .then(([{ sonarsourceSecurity }, response]) => {
        if (!this.mounted) {
          return;
        }

        const hotspots = sortHotspots(response.hotspots, sonarsourceSecurity);

        this.setState({
          hotspots,
          loading: false,
          securityCategories: sonarsourceSecurity,
          selectedHotspotKey: hotspots.length > 0 ? hotspots[0].key : undefined
        });
      })
      .catch(this.handleCallFailure);
  }

  fetchSecurityHotspots() {
    const { branchLike, component, location } = this.props;
    const { filters } = this.state;

    const hotspotKeys = location.query.hotspots
      ? (location.query.hotspots as string).split(',')
      : undefined;

    this.setState({ hotspotKeys });

    if (hotspotKeys && hotspotKeys.length > 0) {
      return getSecurityHotspotList(hotspotKeys);
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
      p: 1,
      ps: PAGE_SIZE,
      status,
      resolution,
      onlyMine: filters.assignedToMe,
      sinceLeakPeriod: filters.newCode,
      ...getBranchLikeQuery(branchLike)
    });
  }

  reloadSecurityHotspotList = () => {
    const { securityCategories } = this.state;

    this.setState({ loading: true });

    return this.fetchSecurityHotspots()
      .then(response => {
        if (!this.mounted) {
          return;
        }

        const hotspots = sortHotspots(response.hotspots, securityCategories);

        this.setState({
          hotspots,
          loading: false,
          selectedHotspotKey: hotspots.length > 0 ? hotspots[0].key : undefined
        });
      })
      .catch(this.handleCallFailure);
  };

  handleChangeFilters = (changes: Partial<HotspotFilters>) => {
    this.setState(
      ({ filters }) => ({ filters: { ...filters, ...changes } }),
      this.reloadSecurityHotspotList
    );
  };

  handleHotspotClick = (key: string) => this.setState({ selectedHotspotKey: key });

  handleHotspotUpdate = ({ key, status, resolution }: HotspotUpdate) => {
    this.setState(({ hotspots }) => {
      const index = hotspots.findIndex(h => h.key === key);

      if (index > -1) {
        const hotspot = {
          ...hotspots[index],
          status,
          resolution
        };

        return { hotspots: [...hotspots.slice(0, index), hotspot, ...hotspots.slice(index + 1)] };
      }
      return null;
    });
  };

  handleShowAllHotspots = () => {
    this.props.router.push({
      ...this.props.location,
      query: { ...this.props.location.query, hotspots: undefined }
    });
  };

  render() {
    const { branchLike } = this.props;
    const {
      hotspotKeys,
      hotspots,
      loading,
      securityCategories,
      selectedHotspotKey,
      filters
    } = this.state;

    return (
      <SecurityHotspotsAppRenderer
        branchLike={branchLike}
        filters={filters}
        hotspots={hotspots}
        isStaticListOfHotspots={Boolean(hotspotKeys && hotspotKeys.length > 0)}
        loading={loading}
        onChangeFilters={this.handleChangeFilters}
        onHotspotClick={this.handleHotspotClick}
        onShowAllHotspots={this.handleShowAllHotspots}
        onUpdateHotspot={this.handleHotspotUpdate}
        securityCategories={securityCategories}
        selectedHotspotKey={selectedHotspotKey}
      />
    );
  }
}

export default withCurrentUser(SecurityHotspotsApp);
