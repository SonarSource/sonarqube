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
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { Hotspot } from '../../../types/security-hotspots';
import Assignee from './assignee/Assignee';
import HotspotSnippetContainer from './HotspotSnippetContainer';
import HotspotViewerTabs from './HotspotViewerTabs';
import Status from './status/Status';

export interface HotspotViewerRendererProps {
  branchLike?: BranchLike;
  hotspot?: Hotspot;
  loading: boolean;
  onUpdateHotspot: () => void;
  securityCategories: T.StandardSecurityCategories;
}

export default function HotspotViewerRenderer(props: HotspotViewerRendererProps) {
  const { branchLike, hotspot, loading, securityCategories } = props;

  return (
    <DeferredSpinner loading={loading}>
      {hotspot && (
        <div className="big-padded">
          <div className="big-spacer-bottom">
            <div className="display-flex-space-between">
              <h1>{hotspot.message}</h1>
            </div>
            <div className="text-muted">
              <span>{translate('category')}:</span>
              <span className="little-spacer-left">
                {securityCategories[hotspot.rule.securityCategory].title}
              </span>
            </div>
          </div>
          <div className="display-flex-row huge-spacer-bottom">
            <Assignee hotspot={hotspot} onAssigneeChange={props.onUpdateHotspot} />
            <Status hotspot={hotspot} onStatusChange={props.onUpdateHotspot} />
          </div>
          <HotspotSnippetContainer branchLike={branchLike} hotspot={hotspot} />
          <HotspotViewerTabs hotspot={hotspot} onUpdateHotspot={props.onUpdateHotspot} />
        </div>
      )}
    </DeferredSpinner>
  );
}
