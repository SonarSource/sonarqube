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
  onClose: () => void;
  onStatusOptionChange: (statusOption: HotspotStatusOption) => Promise<void>;
}

export default function StatusSelection(props: Props) {
  const { hotspot } = props;
  const initialStatus = React.useMemo(
    () => getStatusOptionFromStatusAndResolution(hotspot.status, hotspot.resolution),
    [hotspot],
  );

  const [loading, setLoading] = React.useState(false);
  const [status, setStatus] = React.useState(initialStatus);
  const [comment, setComment] = React.useState('');

  const submitDisabled = status === initialStatus;

  const handleSubmit = async () => {
    const { hotspot } = props;

    if (status !== initialStatus) {
      setLoading(true);
      try {
        await setSecurityHotspotStatus(hotspot.key, {
          ...getStatusAndResolutionFromStatusOption(status),
          comment: comment || undefined,
        });
        await props.onStatusOptionChange(status);

        props.onClose();
      } catch {
        setLoading(false);
      }
    }
  };

  return (
    <StatusSelectionRenderer
      comment={comment}
      loading={loading}
      onCommentChange={(comment) => setComment(comment)}
      onStatusChange={(status) => {
        setStatus(status);
      }}
      onSubmit={handleSubmit}
      onCancel={props.onClose}
      status={status}
      submitDisabled={submitDisabled}
    />
  );
}
