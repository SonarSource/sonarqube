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
import * as React from 'react';
import { getCve } from '../../../api/cves';
import { getRuleDetails } from '../../../api/rules';
import { getSecurityHotspotDetails } from '../../../api/security-hotspots';
import { get } from '../../../helpers/storage';
import { Cve } from '../../../types/cves';
import { Standards } from '../../../types/security';
import {
  Hotspot,
  HotspotStatusFilter,
  HotspotStatusOption,
} from '../../../types/security-hotspots';
import { Component } from '../../../types/types';
import { RuleDescriptionSection } from '../../coding-rules/rule';
import { SHOW_STATUS_DIALOG_STORAGE_KEY } from '../constants';
import { getStatusFilterFromStatusOption } from '../utils';
import HotspotViewerRenderer from './HotspotViewerRenderer';

interface Props {
  component: Component;
  cveId?: string;
  hotspotKey: string;
  hotspotsReviewedMeasure?: string;
  onLocationClick: (index: number) => void;
  onSwitchStatusFilter: (option: HotspotStatusFilter) => void;
  onUpdateHotspot: (hotspotKey: string) => Promise<void>;
  selectedHotspotLocation?: number;
  standards?: Standards;
}

interface State {
  cve?: Cve;
  hotspot?: Hotspot;
  lastStatusChangedTo?: HotspotStatusOption;
  loading: boolean;
  ruleDescriptionSections?: RuleDescriptionSection[];
  ruleLanguage?: string;
  showStatusUpdateSuccessModal: boolean;
}

export default class HotspotViewer extends React.PureComponent<Props, State> {
  mounted = false;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = { loading: false, showStatusUpdateSuccessModal: false };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchHotspot();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.hotspotKey !== this.props.hotspotKey) {
      this.fetchHotspot();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchHotspot = async () => {
    this.setState({ loading: true });

    try {
      const hotspot = await getSecurityHotspotDetails(this.props.hotspotKey);
      const ruleDetails = await getRuleDetails({ key: hotspot.rule.key, organization: this.props.component.organization }).then((r) => r.rule);
      let cve;
      if (typeof this.props.cveId === 'string') {
        cve = await getCve(this.props.cveId);
      }

      if (this.mounted) {
        this.setState({
          hotspot,
          loading: false,
          ruleLanguage: ruleDetails.lang,
          ruleDescriptionSections: ruleDetails.descriptionSections,
          cve,
        });
      }
    } catch (error) {
      if (this.mounted) {
        this.setState({ loading: false });
      }
    }
  };

  handleHotspotUpdate = async (statusUpdate = false, statusOption?: HotspotStatusOption) => {
    const { hotspotKey } = this.props;

    if (statusUpdate) {
      this.setState({
        lastStatusChangedTo: statusOption,
        showStatusUpdateSuccessModal: get(SHOW_STATUS_DIALOG_STORAGE_KEY) !== 'false',
      });
      await this.props.onUpdateHotspot(hotspotKey);
    } else {
      await this.fetchHotspot();
    }
  };

  handleSwitchFilterToStatusOfUpdatedHotspot = () => {
    const { lastStatusChangedTo } = this.state;

    if (lastStatusChangedTo) {
      this.props.onSwitchStatusFilter(getStatusFilterFromStatusOption(lastStatusChangedTo));
    }
  };

  handleCloseStatusUpdateSuccessModal = () => {
    this.setState({ showStatusUpdateSuccessModal: false });
  };

  render() {
    const { component, hotspotsReviewedMeasure, selectedHotspotLocation, standards } = this.props;

    const {
      hotspot,
      ruleDescriptionSections,
      ruleLanguage,
      cve,
      loading,
      showStatusUpdateSuccessModal,
      lastStatusChangedTo,
    } = this.state;

    return (
      <HotspotViewerRenderer
        component={component}
        hotspot={hotspot}
        hotspotsReviewedMeasure={hotspotsReviewedMeasure}
        lastStatusChangedTo={lastStatusChangedTo}
        loading={loading}
        onCloseStatusUpdateSuccessModal={this.handleCloseStatusUpdateSuccessModal}
        onLocationClick={this.props.onLocationClick}
        onSwitchFilterToStatusOfUpdatedHotspot={this.handleSwitchFilterToStatusOfUpdatedHotspot}
        onUpdateHotspot={this.handleHotspotUpdate}
        ruleDescriptionSections={ruleDescriptionSections}
        ruleLanguage={ruleLanguage}
        cve={cve}
        selectedHotspotLocation={selectedHotspotLocation}
        showStatusUpdateSuccessModal={showStatusUpdateSuccessModal}
        standards={standards}
      />
    );
  }
}
