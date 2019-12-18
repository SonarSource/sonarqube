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
import * as React from 'react';
import { addNoFooterPageClass, removeNoFooterPageClass } from 'sonar-ui-common/helpers/pages';
import { getSecurityHotspots } from '../../api/security-hotspots';
import { getBranchLikeQuery } from '../../helpers/branch-like';
import { getStandards } from '../../helpers/security-standard';
import { BranchLike } from '../../types/branch-like';
import { HotspotUpdate, RawHotspot } from '../../types/security-hotspots';
import SecurityHotspotsAppRenderer from './SecurityHotspotsAppRenderer';
import './styles.css';
import { sortHotspots } from './utils';

const PAGE_SIZE = 500;

interface Props {
  branchLike?: BranchLike;
  component: T.Component;
}

interface State {
  hotspots: RawHotspot[];
  loading: boolean;
  securityCategories: T.StandardSecurityCategories;
  selectedHotspotKey: string | undefined;
}

export default class SecurityHotspotsApp extends React.PureComponent<Props, State> {
  mounted = false;
  state = {
    loading: true,
    hotspots: [],
    securityCategories: {},
    selectedHotspotKey: undefined
  };

  componentDidMount() {
    this.mounted = true;
    addNoFooterPageClass();
    this.fetchInitialData();
  }

  componentDidUpdate(previous: Props) {
    if (this.props.component.key !== previous.component.key) {
      this.fetchInitialData();
    }
  }

  componentWillUnmount() {
    removeNoFooterPageClass();
    this.mounted = false;
  }

  fetchInitialData() {
    const { branchLike, component } = this.props;

    return Promise.all([
      getStandards(),
      getSecurityHotspots({
        projectKey: component.key,
        p: 1,
        ps: PAGE_SIZE,
        ...getBranchLikeQuery(branchLike)
      })
    ])
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
      .catch(() => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      });
  }

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

  render() {
    const { branchLike } = this.props;
    const { hotspots, loading, securityCategories, selectedHotspotKey } = this.state;

    return (
      <SecurityHotspotsAppRenderer
        branchLike={branchLike}
        hotspots={hotspots}
        loading={loading}
        onHotspotClick={this.handleHotspotClick}
        onUpdateHotspot={this.handleHotspotUpdate}
        securityCategories={securityCategories}
        selectedHotspotKey={selectedHotspotKey}
      />
    );
  }
}
