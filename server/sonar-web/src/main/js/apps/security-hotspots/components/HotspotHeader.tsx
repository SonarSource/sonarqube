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
import {
  ClipboardIconButton,
  IssueMessageHighlighting,
  LightLabel,
  LightPrimary,
  Link,
  LinkIcon,
  StyledPageTitle,
} from 'design-system';
import React from 'react';
import { getComponentSecurityHotspotsUrl } from '~sonar-aligned/helpers/urls';
import { translate } from '../../../helpers/l10n';
import { getPathUrlAsString, getRuleUrl } from '../../../helpers/urls';
import { useRefreshBranchStatus } from '../../../queries/branch';
import { BranchLike } from '../../../types/branch-like';
import { SecurityStandard, Standards } from '../../../types/security';
import { Hotspot, HotspotStatusOption } from '../../../types/security-hotspots';
import { Component } from '../../../types/types';
import HotspotHeaderRightSection from './HotspotHeaderRightSection';
import Status from './status/Status';

export interface HotspotHeaderProps {
  branchLike?: BranchLike;
  component: Component;
  hotspot: Hotspot;
  onUpdateHotspot: (statusUpdate?: boolean, statusOption?: HotspotStatusOption) => Promise<void>;
  standards?: Standards;
}

export function HotspotHeader(props: HotspotHeaderProps) {
  const { branchLike, component, hotspot, standards } = props;
  const { message, messageFormattings, rule, key } = hotspot;
  const refreshBranchStatus = useRefreshBranchStatus();

  const permalink = getPathUrlAsString(
    getComponentSecurityHotspotsUrl(component.key, branchLike, {
      hotspots: key,
    }),
    false,
  );

  const categoryStandard = standards?.[SecurityStandard.SONARSOURCE][rule.securityCategory]?.title;
  const handleStatusChange = async (statusOption: HotspotStatusOption) => {
    await props.onUpdateHotspot(true, statusOption);
    refreshBranchStatus();
  };

  return (
    <div>
      <div className="sw-flex sw-justify-between sw-gap-8 hotspot-header">
        <div className="sw-flex-1">
          <StyledPageTitle as="h1" className="sw-whitespace-normal sw-overflow-visible">
            <LightPrimary>
              <IssueMessageHighlighting message={message} messageFormattings={messageFormattings} />
            </LightPrimary>
            <ClipboardIconButton
              Icon={LinkIcon}
              copiedLabel={translate('copied_action')}
              copyLabel={translate('copy_to_clipboard')}
              className="sw-ml-2"
              copyValue={permalink}
              discreet
            />
          </StyledPageTitle>
          <div className="sw-mt-2 sw-mb-4 sw-body-sm">
            <LightLabel>{rule.name}</LightLabel>
            <Link className="sw-ml-1" to={getRuleUrl(rule.key)} target="_blank">
              {rule.key}
            </Link>
          </div>
          <Status hotspot={hotspot} onStatusChange={handleStatusChange} />
        </div>
        <div className="sw-flex sw-flex-col sw-gap-4">
          <HotspotHeaderRightSection
            hotspot={hotspot}
            categoryStandard={categoryStandard}
            onUpdateHotspot={props.onUpdateHotspot}
          />
        </div>
      </div>
    </div>
  );
}
