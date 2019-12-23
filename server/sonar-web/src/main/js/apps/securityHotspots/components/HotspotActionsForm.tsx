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
import { assignSecurityHotspot, setSecurityHotspotStatus } from '../../../api/security-hotspots';
import {
  HotspotResolution,
  HotspotSetStatusRequest,
  HotspotStatus,
  HotspotStatusOptions,
  HotspotUpdateFields
} from '../../../types/security-hotspots';
import HotspotActionsFormRenderer from './HotspotActionsFormRenderer';

interface Props {
  hotspotKey: string;
  onSubmit: (data: HotspotUpdateFields) => void;
}

interface State {
  comment: string;
  selectedUser?: T.UserActive;
  selectedOption: HotspotStatusOptions;
  submitting: boolean;
}

export default class HotspotActionsForm extends React.Component<Props, State> {
  state: State = {
    comment: '',
    selectedOption: HotspotStatusOptions.FIXED,
    submitting: false
  };

  handleSelectOption = (selectedOption: HotspotStatusOptions) => {
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

    const { hotspotKey } = this.props;
    const { comment, selectedOption, selectedUser } = this.state;

    const status =
      selectedOption === HotspotStatusOptions.ADDITIONAL_REVIEW
        ? HotspotStatus.TO_REVIEW
        : HotspotStatus.REVIEWED;
    const data: HotspotSetStatusRequest = { status };

    // If reassigning, ignore comment for status update. It will be sent with the reassignment below
    if (comment && !(selectedOption === HotspotStatusOptions.ADDITIONAL_REVIEW && selectedUser)) {
      data.comment = comment;
    }

    if (selectedOption !== HotspotStatusOptions.ADDITIONAL_REVIEW) {
      data.resolution = HotspotResolution[selectedOption];
    }

    this.setState({ submitting: true });
    return setSecurityHotspotStatus(hotspotKey, data)
      .then(() => {
        if (selectedOption === HotspotStatusOptions.ADDITIONAL_REVIEW && selectedUser) {
          return this.assignHotspot(selectedUser, comment);
        }
        return null;
      })
      .then(() => {
        this.props.onSubmit({ status, resolution: data.resolution });
      })
      .catch(() => {
        this.setState({ submitting: false });
      });
  };

  assignHotspot = (assignee: T.UserActive, comment: string) => {
    const { hotspotKey } = this.props;

    return assignSecurityHotspot(hotspotKey, {
      assignee: assignee.login,
      comment
    });
  };

  render() {
    const { hotspotKey } = this.props;
    const { comment, selectedOption, selectedUser, submitting } = this.state;

    return (
      <HotspotActionsFormRenderer
        comment={comment}
        hotspotKey={hotspotKey}
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
