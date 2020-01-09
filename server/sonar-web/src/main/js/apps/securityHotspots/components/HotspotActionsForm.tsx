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
import {
  assignSecurityHotspot,
  commentSecurityHotspot,
  setSecurityHotspotStatus
} from '../../../api/security-hotspots';
import {
  Hotspot,
  HotspotResolution,
  HotspotStatus,
  HotspotStatusOption
} from '../../../types/security-hotspots';
import HotspotActionsFormRenderer from './HotspotActionsFormRenderer';

interface Props {
  hotspot: Hotspot;
  onSubmit: () => void;
}

interface State {
  comment: string;
  selectedOption: HotspotStatusOption;
  selectedUser?: T.UserActive;
  submitting: boolean;
}

export default class HotspotActionsForm extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);

    let selectedOption = HotspotStatusOption.FIXED;
    if (props.hotspot.status === HotspotStatus.TO_REVIEW) {
      selectedOption = HotspotStatusOption.ADDITIONAL_REVIEW;
    } else if (props.hotspot.resolution) {
      selectedOption = HotspotStatusOption[props.hotspot.resolution];
    }

    this.state = {
      comment: '',
      selectedOption,
      submitting: false
    };
  }

  handleSelectOption = (selectedOption: HotspotStatusOption) => {
    this.setState({ selectedOption });
  };

  handleAssign = (selectedUser: T.UserActive) => {
    this.setState({ selectedUser });
  };

  handleCommentChange = (comment: string) => {
    this.setState({ comment });
  };

  handleSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();

    const { hotspot } = this.props;
    const { comment, selectedOption, selectedUser } = this.state;

    const status =
      selectedOption === HotspotStatusOption.ADDITIONAL_REVIEW
        ? HotspotStatus.TO_REVIEW
        : HotspotStatus.REVIEWED;

    const resolution =
      selectedOption !== HotspotStatusOption.ADDITIONAL_REVIEW
        ? HotspotResolution[selectedOption]
        : undefined;

    this.setState({ submitting: true });
    /*
     *  updateAssignee depends on updateStatus, hence these are chained rather than
     *  run in parallel. The comment should also appear last in the changelog.
     */
    return Promise.resolve()
      .then(() => this.updateStatus(hotspot, status, resolution))
      .then(() => this.updateAssignee(hotspot, selectedOption, selectedUser))
      .then(() => this.addComment(hotspot, comment))
      .then(() => {
        this.props.onSubmit();
        // No need to set "submitting", we are closing the window
      })
      .catch(() => {
        this.setState({ submitting: false });
      });
  };

  updateStatus = (hotspot: Hotspot, status: HotspotStatus, resolution?: HotspotResolution) => {
    if (
      hotspot.canChangeStatus &&
      (status !== hotspot.status || resolution !== hotspot.resolution)
    ) {
      return setSecurityHotspotStatus(hotspot.key, { status, resolution });
    }

    return Promise.resolve();
  };

  updateAssignee = (
    hotspot: Hotspot,
    selectedOption: HotspotStatusOption,
    selectedUser?: T.UserActive
  ) => {
    if (
      selectedOption === HotspotStatusOption.ADDITIONAL_REVIEW &&
      selectedUser &&
      selectedUser.login !== hotspot.assignee
    ) {
      return assignSecurityHotspot(hotspot.key, {
        assignee: selectedUser.login
      });
    }
    return Promise.resolve();
  };

  addComment = (hotspot: Hotspot, comment: string) => {
    if (comment.length > 0) {
      return commentSecurityHotspot(hotspot.key, comment);
    }
    return Promise.resolve();
  };

  render() {
    const { hotspot } = this.props;
    const { comment, selectedOption, selectedUser, submitting } = this.state;

    return (
      <HotspotActionsFormRenderer
        comment={comment}
        hotspot={hotspot}
        onAssign={this.handleAssign}
        onChangeComment={this.handleCommentChange}
        onSelectOption={this.handleSelectOption}
        onSubmit={this.handleSubmit}
        selectedOption={selectedOption}
        selectedUser={selectedUser}
        submitting={submitting}
      />
    );
  }
}
