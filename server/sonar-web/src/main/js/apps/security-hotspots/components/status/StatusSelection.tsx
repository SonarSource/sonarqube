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
  issueResolutionExpiryDate?: string;
}
export default function StatusSelection(props: Props) {
  const { hotspot } = props;

  const initialStatus = React.useMemo(
    () => getStatusOptionFromStatusAndResolution(hotspot.status, hotspot.resolution),
    [hotspot],
  );

  const originalExpiryDate = React.useMemo(() => {
    if (typeof hotspot.issueResolutionExpiresAt === "number" && hotspot.issueResolutionExpiresAt > 0) {
      const d = new Date(hotspot.issueResolutionExpiresAt);
      const yyyy = d.getFullYear();
      const mm = String(d.getMonth() + 1).padStart(2, "0");
      const dd = String(d.getDate()).padStart(2, "0");
      return `${yyyy}-${mm}-${dd}`;
    }
    return undefined;
  }, [hotspot]);


  const originalComment = "";

  const [loading, setLoading] = React.useState(false);
  const [status, setStatus] = React.useState(initialStatus);
  const [comment, setComment] = React.useState('');
  const [expiryDate, setExpiryDate] = React.useState<string | undefined>(originalExpiryDate);

  const submitDisabled =
    status === initialStatus &&
    comment === originalComment &&
    expiryDate === originalExpiryDate;

  const handleSubmit = async () => {
    if (!submitDisabled) {
      setLoading(true);
      try {
        await setSecurityHotspotStatus(hotspot.key, {
          ...getStatusAndResolutionFromStatusOption(status),
          comment: comment || undefined,
          issueResolutionExpiryDate: expiryDate || "",
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
      expiryDate={expiryDate}
      loading={loading}
      onCommentChange={(comment) => setComment(comment)}
      onStatusChange={(status) => {
        setStatus(status);
      }}
      onExpiryDateChange={setExpiryDate}
      onSubmit={handleSubmit}
      onCancel={props.onClose}
      status={status}
      submitDisabled={submitDisabled}
      hotspot={hotspot}
    />
  );
}
