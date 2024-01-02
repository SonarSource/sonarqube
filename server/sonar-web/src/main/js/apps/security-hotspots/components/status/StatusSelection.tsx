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
import { setSecurityHotspotStatus } from '../../../../api/security-hotspots';
import { Hotspot, HotspotStatusOption } from '../../../../types/security-hotspots';
import {
  getStatusAndResolutionFromStatusOption,
  getStatusOptionFromStatusAndResolution,
} from '../../utils';
import StatusSelectionRenderer from './StatusSelectionRenderer';

interface Props {
  hotspot: Hotspot;
  onStatusOptionChange: (statusOption: HotspotStatusOption) => Promise<void>;
  comment: string;
  setComment: (comment: string) => void;
}

interface State {
  loading: boolean;
  initialStatus: HotspotStatusOption;
  selectedStatus: HotspotStatusOption;
}

export default class StatusSelection extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);

    const initialStatus = getStatusOptionFromStatusAndResolution(
      props.hotspot.status,
      props.hotspot.resolution
    );

    this.state = {
      loading: false,
      initialStatus,
      selectedStatus: initialStatus,
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleStatusChange = (selectedStatus: HotspotStatusOption) => {
    this.setState({ selectedStatus });
  };

  handleCommentChange = (comment: string) => {
    this.props.setComment(comment);
  };

  handleSubmit = () => {
    const { hotspot, comment } = this.props;
    const { initialStatus, selectedStatus } = this.state;

    if (selectedStatus && selectedStatus !== initialStatus) {
      this.setState({ loading: true });
      setSecurityHotspotStatus(hotspot.key, {
        ...getStatusAndResolutionFromStatusOption(selectedStatus),
        comment: comment || undefined,
      })
        .then(async () => {
          await this.props.onStatusOptionChange(selectedStatus);
          if (this.mounted) {
            this.setState({ loading: false });
          }
        })
        .catch(() => this.setState({ loading: false }));
    }
  };

  render() {
    const { comment } = this.props;
    const { initialStatus, loading, selectedStatus } = this.state;
    const submitDisabled = selectedStatus === initialStatus;

    return (
      <StatusSelectionRenderer
        comment={comment}
        loading={loading}
        onCommentChange={this.handleCommentChange}
        onStatusChange={this.handleStatusChange}
        onSubmit={this.handleSubmit}
        selectedStatus={selectedStatus}
        submitDisabled={submitDisabled}
      />
    );
  }
}
