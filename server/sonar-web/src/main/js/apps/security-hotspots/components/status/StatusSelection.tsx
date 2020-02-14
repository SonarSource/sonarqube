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
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { setSecurityHotspotStatus } from '../../../../api/security-hotspots';
import addGlobalSuccessMessage from '../../../../app/utils/addGlobalSuccessMessage';
import { Hotspot, HotspotStatusOption } from '../../../../types/security-hotspots';
import {
  getStatusAndResolutionFromStatusOption,
  getStatusOptionFromStatusAndResolution
} from '../../utils';
import StatusSelectionRenderer from './StatusSelectionRenderer';

interface Props {
  hotspot: Hotspot;
  onStatusOptionChange: (statusOption: HotspotStatusOption) => void;
}

interface State {
  comment?: string;
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
      selectedStatus: initialStatus
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
    this.setState({ comment });
  };

  handleSubmit = () => {
    const { hotspot } = this.props;
    const { comment, initialStatus, selectedStatus } = this.state;

    if (selectedStatus && selectedStatus !== initialStatus) {
      this.setState({ loading: true });
      setSecurityHotspotStatus(hotspot.key, {
        ...getStatusAndResolutionFromStatusOption(selectedStatus),
        comment: comment || undefined
      })
        .then(() => {
          this.setState({ loading: false });
          this.props.onStatusOptionChange(selectedStatus);
        })
        .then(() =>
          addGlobalSuccessMessage(
            translateWithParameters(
              'hotspots.update.success',
              translate('hotspots.status_option', selectedStatus)
            )
          )
        )
        .catch(() => this.setState({ loading: false }));
    }
  };

  render() {
    const { comment, initialStatus, loading, selectedStatus } = this.state;
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
