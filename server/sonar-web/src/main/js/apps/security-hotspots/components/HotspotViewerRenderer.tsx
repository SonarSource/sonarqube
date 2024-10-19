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
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import { fillBranchLike } from '../../../helpers/branch-like';
import { Standards } from '../../../types/security';
import { Hotspot, HotspotStatusOption } from '../../../types/security-hotspots';
import { Component } from '../../../types/types';
import { HotspotHeader } from './HotspotHeader';

import { Spinner } from 'design-system';
import { Cve } from '../../../types/cves';
import { CurrentUser } from '../../../types/users';
import { RuleDescriptionSection } from '../../coding-rules/rule';
import HotspotReviewHistoryAndComments from './HotspotReviewHistoryAndComments';
import HotspotSnippetContainer from './HotspotSnippetContainer';
import './HotspotViewer.css';
import HotspotViewerTabs from './HotspotViewerTabs';
import StatusUpdateSuccessModal from './StatusUpdateSuccessModal';

export interface HotspotViewerRendererProps {
  component: Component;
  currentUser: CurrentUser;
  cve?: Cve;
  hotspot?: Hotspot;
  hotspotsReviewedMeasure?: string;
  lastStatusChangedTo?: HotspotStatusOption;
  loading: boolean;
  onCloseStatusUpdateSuccessModal: () => void;
  onLocationClick: (index: number) => void;
  onSwitchFilterToStatusOfUpdatedHotspot: () => void;
  onUpdateHotspot: (statusUpdate?: boolean, statusOption?: HotspotStatusOption) => Promise<void>;
  ruleDescriptionSections?: RuleDescriptionSection[];
  ruleLanguage?: string;
  selectedHotspotLocation?: number;
  showStatusUpdateSuccessModal: boolean;
  standards?: Standards;
}

export function HotspotViewerRenderer(props: HotspotViewerRendererProps) {
  const {
    component,
    currentUser,
    hotspot,
    hotspotsReviewedMeasure,
    lastStatusChangedTo,
    loading,
    ruleDescriptionSections,
    ruleLanguage,
    cve,
    selectedHotspotLocation,
    showStatusUpdateSuccessModal,
    standards,
  } = props;

  const branchLike = hotspot && fillBranchLike(hotspot.project.branch, hotspot.project.pullRequest);

  return (
    <>
      <Spinner className="sw-ml-4 sw-mt-4" loading={loading} />

      {showStatusUpdateSuccessModal && (
        <StatusUpdateSuccessModal
          hotspotsReviewedMeasure={hotspotsReviewedMeasure}
          lastStatusChangedTo={lastStatusChangedTo}
          onClose={props.onCloseStatusUpdateSuccessModal}
          onSwitchFilterToStatusOfUpdatedHotspot={props.onSwitchFilterToStatusOfUpdatedHotspot}
        />
      )}

      {hotspot && (
        <div className="sw-box-border sw-p-6">
          <HotspotHeader
            branchLike={branchLike}
            component={component}
            hotspot={hotspot}
            onUpdateHotspot={props.onUpdateHotspot}
            standards={standards}
          />
          <HotspotViewerTabs
            activityTabContent={
              <HotspotReviewHistoryAndComments
                currentUser={currentUser}
                hotspot={hotspot}
                onCommentUpdate={props.onUpdateHotspot}
              />
            }
            codeTabContent={
              <HotspotSnippetContainer
                branchLike={branchLike}
                component={component}
                hotspot={hotspot}
                onLocationSelect={props.onLocationClick}
                selectedHotspotLocation={selectedHotspotLocation}
              />
            }
            hotspot={hotspot}
            onUpdateHotspot={props.onUpdateHotspot}
            ruleDescriptionSections={ruleDescriptionSections}
            ruleLanguage={ruleLanguage}
            cve={cve}
          />
        </div>
      )}
    </>
  );
}

export default withCurrentUserContext(HotspotViewerRenderer);
