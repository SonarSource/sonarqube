/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { fillBranchLike } from '../../../helpers/branch-like';
import { Standards } from '../../../types/security';
import { Hotspot, HotspotStatusOption } from '../../../types/security-hotspots';
import { Component } from '../../../types/types';
import { CurrentUser } from '../../../types/users';
import { RuleDescriptionSection } from '../../coding-rules/rule';
import { HotspotHeader } from './HotspotHeader';
import HotspotReviewHistoryAndComments from './HotspotReviewHistoryAndComments';
import HotspotSnippetContainer from './HotspotSnippetContainer';
import './HotspotViewer.css';
import HotspotViewerTabs from './HotspotViewerTabs';

export interface HotspotViewerRendererProps {
  component: Component;
  currentUser: CurrentUser;
  hotspot?: Hotspot;
  ruleDescriptionSections?: RuleDescriptionSection[];
  loading: boolean;
  commentTextRef: React.RefObject<HTMLTextAreaElement>;
  onUpdateHotspot: (statusUpdate?: boolean, statusOption?: HotspotStatusOption) => Promise<void>;
  onShowCommentForm: () => void;
  onLocationClick: (index: number) => void;
  selectedHotspotLocation?: number;
  standards?: Standards;
}

export function HotspotViewerRenderer(props: HotspotViewerRendererProps) {
  const {
    component,
    currentUser,
    hotspot,
    loading,
    commentTextRef,
    selectedHotspotLocation,
    ruleDescriptionSections,
    standards,
  } = props;

  const branchLike = hotspot && fillBranchLike(hotspot.project.branch, hotspot.project.pullRequest);

  return (
    <DeferredSpinner className="big-spacer-left big-spacer-top" loading={loading}>
      {hotspot && (
        <div className="sw-box-border sw-p-6">
          <HotspotHeader
            hotspot={hotspot}
            component={component}
            standards={standards}
            onUpdateHotspot={props.onUpdateHotspot}
            branchLike={branchLike}
          />
          <HotspotViewerTabs
            codeTabContent={
              <HotspotSnippetContainer
                branchLike={branchLike}
                component={component}
                hotspot={hotspot}
                onCommentButtonClick={props.onShowCommentForm}
                onLocationSelect={props.onLocationClick}
                selectedHotspotLocation={selectedHotspotLocation}
              />
            }
            hotspot={hotspot}
            ruleDescriptionSections={ruleDescriptionSections}
            selectedHotspotLocation={selectedHotspotLocation}
          />
          <HotspotReviewHistoryAndComments
            commentTextRef={commentTextRef}
            currentUser={currentUser}
            hotspot={hotspot}
            onCommentUpdate={props.onUpdateHotspot}
          />
        </div>
      )}
    </DeferredSpinner>
  );
}

export default withCurrentUserContext(HotspotViewerRenderer);
