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
import { getRuleDetails } from '../../../api/rules';
import { getSecurityHotspotDetails } from '../../../api/security-hotspots';
import { scrollToElement } from '../../../helpers/scrolling';
import {
  Hotspot,
  HotspotStatusFilter,
  HotspotStatusOption,
} from '../../../types/security-hotspots';
import { Component } from '../../../types/types';
import { RuleDescriptionSection } from '../../coding-rules/rule';
import { getStatusFilterFromStatusOption } from '../utils';
import HotspotViewerRenderer from './HotspotViewerRenderer';

interface Props {
  component: Component;
  hotspotKey: string;
  hotspotsReviewedMeasure?: string;
  onSwitchStatusFilter: (option: HotspotStatusFilter) => void;
  onUpdateHotspot: (hotspotKey: string) => Promise<void>;
  onLocationClick: (index: number) => void;
  selectedHotspotLocation?: number;
}

interface State {
  hotspot?: Hotspot;
  ruleDescriptionSections?: RuleDescriptionSection[];
  lastStatusChangedTo?: HotspotStatusOption;
  loading: boolean;
  showStatusUpdateSuccessModal: boolean;
}

const SCROLL_TO_COMMENT_BOTTOM_OFFSET = 300;

export default class HotspotViewer extends React.PureComponent<Props, State> {
  mounted = false;
  state: State;
  commentTextRef: React.RefObject<HTMLTextAreaElement>;

  constructor(props: Props) {
    super(props);
    this.commentTextRef = React.createRef<HTMLTextAreaElement>();
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
      const ruleDetails = await getRuleDetails({ key: hotspot.rule.key }).then((r) => r.rule);

      if (this.mounted) {
        this.setState({
          hotspot,
          loading: false,
          ruleDescriptionSections: ruleDetails.descriptionSections,
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
      this.setState({ lastStatusChangedTo: statusOption, showStatusUpdateSuccessModal: true });
      await this.props.onUpdateHotspot(hotspotKey);
    } else {
      await this.fetchHotspot();
    }
  };

  handleScrollToCommentForm = () => {
    if (this.commentTextRef.current) {
      this.commentTextRef.current.focus({ preventScroll: true });
      scrollToElement(this.commentTextRef.current, {
        bottomOffset: SCROLL_TO_COMMENT_BOTTOM_OFFSET,
      });
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
    const { component, hotspotsReviewedMeasure, selectedHotspotLocation } = this.props;
    const {
      hotspot,
      ruleDescriptionSections,
      lastStatusChangedTo,
      loading,
      showStatusUpdateSuccessModal,
    } = this.state;

    return (
      <HotspotViewerRenderer
        component={component}
        commentTextRef={this.commentTextRef}
        hotspot={hotspot}
        ruleDescriptionSections={ruleDescriptionSections}
        hotspotsReviewedMeasure={hotspotsReviewedMeasure}
        lastStatusChangedTo={lastStatusChangedTo}
        loading={loading}
        onCloseStatusUpdateSuccessModal={this.handleCloseStatusUpdateSuccessModal}
        onSwitchFilterToStatusOfUpdatedHotspot={this.handleSwitchFilterToStatusOfUpdatedHotspot}
        onShowCommentForm={this.handleScrollToCommentForm}
        onUpdateHotspot={this.handleHotspotUpdate}
        onLocationClick={this.props.onLocationClick}
        showStatusUpdateSuccessModal={showStatusUpdateSuccessModal}
        selectedHotspotLocation={selectedHotspotLocation}
      />
    );
  }
}
