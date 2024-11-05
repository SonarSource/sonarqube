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

import { useState } from 'react';
import { get } from '../../../helpers/storage';
import { useSecurityHotspotDetailsQuery } from '../../../queries/hotspots';
import { useRuleDetailsQuery } from '../../../queries/rules';
import { Standards } from '../../../types/security';
import { HotspotStatusFilter, HotspotStatusOption } from '../../../types/security-hotspots';
import { Component } from '../../../types/types';
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

export default function HotspotViewer(props: Readonly<Props>) {
  const {
    hotspotKey,
    component,
    cveId,
    hotspotsReviewedMeasure,
    selectedHotspotLocation,
    standards,
  } = props;

  const [lastStatusChangedTo, setLastStatusChangedTo] = useState<HotspotStatusOption>();
  const [showStatusUpdateSuccessModal, setShowStatusUpdateSuccessModal] = useState(false);

  const { data: hotspot, refetch } = useSecurityHotspotDetailsQuery({ key: hotspotKey });
  const { data: rule, isLoading } = useRuleDetailsQuery(
    { key: hotspot?.rule.key! },
    { enabled: hotspot !== undefined },
  );

  const ruleLanguage = rule?.rule.lang;
  const ruleDescriptionSections = rule?.rule.descriptionSections;

  const handleHotspotUpdate = async (statusUpdate = false, statusOption?: HotspotStatusOption) => {
    if (statusUpdate) {
      setLastStatusChangedTo(statusOption);
      setShowStatusUpdateSuccessModal(get(SHOW_STATUS_DIALOG_STORAGE_KEY) !== 'false');
      await props.onUpdateHotspot(hotspotKey);
    } else {
      refetch();
    }
  };

  const handleSwitchFilterToStatusOfUpdatedHotspot = () => {
    if (lastStatusChangedTo) {
      props.onSwitchStatusFilter(getStatusFilterFromStatusOption(lastStatusChangedTo));
    }
  };

  const handleCloseStatusUpdateSuccessModal = () => {
    setShowStatusUpdateSuccessModal(false);
  };

  return (
    <HotspotViewerRenderer
      component={component}
      hotspot={hotspot}
      hotspotsReviewedMeasure={hotspotsReviewedMeasure}
      lastStatusChangedTo={lastStatusChangedTo}
      loading={isLoading}
      onCloseStatusUpdateSuccessModal={handleCloseStatusUpdateSuccessModal}
      onLocationClick={props.onLocationClick}
      onSwitchFilterToStatusOfUpdatedHotspot={handleSwitchFilterToStatusOfUpdatedHotspot}
      onUpdateHotspot={handleHotspotUpdate}
      ruleDescriptionSections={ruleDescriptionSections}
      ruleLanguage={ruleLanguage}
      cveId={cveId}
      selectedHotspotLocation={selectedHotspotLocation}
      showStatusUpdateSuccessModal={showStatusUpdateSuccessModal}
      standards={standards}
    />
  );
}
