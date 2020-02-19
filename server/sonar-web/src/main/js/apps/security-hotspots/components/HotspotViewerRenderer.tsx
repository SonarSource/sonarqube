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
import * as classNames from 'classnames';
import * as React from 'react';
import { ClipboardButton } from 'sonar-ui-common/components/controls/clipboard';
import LinkIcon from 'sonar-ui-common/components/icons/LinkIcon';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getPathUrlAsString } from 'sonar-ui-common/helpers/urls';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { getComponentSecurityHotspotsUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { Hotspot } from '../../../types/security-hotspots';
import Assignee from './assignee/Assignee';
import HotspotSnippetContainer from './HotspotSnippetContainer';
import './HotspotViewer.css';
import HotspotViewerTabs from './HotspotViewerTabs';
import Status from './status/Status';

export interface HotspotViewerRendererProps {
  branchLike?: BranchLike;
  component: T.Component;
  hotspot?: Hotspot;
  loading: boolean;
  onUpdateHotspot: () => Promise<void>;
  securityCategories: T.StandardSecurityCategories;
}

export default function HotspotViewerRenderer(props: HotspotViewerRendererProps) {
  const { branchLike, component, hotspot, loading, securityCategories } = props;

  const permalink = getPathUrlAsString(
    getComponentSecurityHotspotsUrl(component.key, {
      ...getBranchLikeQuery(branchLike),
      hotspots: hotspot?.key
    }),
    false
  );

  return (
    <DeferredSpinner loading={loading}>
      {hotspot && (
        <div className="big-padded">
          <div className="huge-spacer-bottom display-flex-space-between">
            <strong className="big big-spacer-right">{hotspot.message}</strong>
            <ClipboardButton copyValue={permalink}>
              <LinkIcon className="spacer-right" />
              <span>{translate('hotspots.get_permalink')}</span>
            </ClipboardButton>
          </div>

          <div className="huge-spacer-bottom display-flex-row">
            <div className="hotspot-information display-flex-column display-flex-space-between">
              <div className="display-flex-center">
                <span className="big-spacer-right">{translate('category')}</span>
                <strong className="nowrap">
                  {securityCategories[hotspot.rule.securityCategory].title}
                </strong>
              </div>
              <div className="display-flex-center">
                <span className="big-spacer-right">{translate('hotspots.risk_exposure')}</span>
                <div
                  className={classNames(
                    'hotspot-risk-badge',
                    hotspot.rule.vulnerabilityProbability
                  )}>
                  {translate('risk_exposure', hotspot.rule.vulnerabilityProbability)}
                </div>
              </div>
              <div className="display-flex-center">
                <span className="big-spacer-right">{translate('assignee')}</span>
                <div>
                  <Assignee hotspot={hotspot} onAssigneeChange={props.onUpdateHotspot} />
                </div>
              </div>
            </div>
            <div className="huge-spacer-left">
              <Status hotspot={hotspot} onStatusChange={props.onUpdateHotspot} />
            </div>
          </div>

          <HotspotSnippetContainer branchLike={branchLike} hotspot={hotspot} />
          <HotspotViewerTabs hotspot={hotspot} onUpdateHotspot={props.onUpdateHotspot} />
        </div>
      )}
    </DeferredSpinner>
  );
}
