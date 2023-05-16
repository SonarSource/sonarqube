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
import { withTheme } from '@emotion/react';
import styled from '@emotion/styled';
import {
  ClipboardIconButton,
  LAYOUT_GLOBAL_NAV_HEIGHT,
  LAYOUT_PROJECT_NAV_HEIGHT,
  LightLabel,
  LightPrimary,
  Link,
  LinkIcon,
  StyledPageTitle,
  themeColor,
} from 'design-system';
import React from 'react';
import { IssueMessageHighlighting } from '../../../components/issue/IssueMessageHighlighting';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import {
  getComponentSecurityHotspotsUrl,
  getPathUrlAsString,
  getRuleUrl,
} from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { SecurityStandard, Standards } from '../../../types/security';
import { Hotspot, HotspotStatusOption } from '../../../types/security-hotspots';
import { Component } from '../../../types/types';
import HotspotHeaderRightSection from './HotspotHeaderRightSection';
import Status from './status/Status';

export interface HotspotHeaderProps {
  hotspot: Hotspot;
  component: Component;
  branchLike?: BranchLike;
  standards?: Standards;
  onUpdateHotspot: (statusUpdate?: boolean, statusOption?: HotspotStatusOption) => Promise<void>;
}

export function HotspotHeader(props: HotspotHeaderProps) {
  const { hotspot, component, branchLike, standards } = props;
  const { message, messageFormattings, rule, key } = hotspot;

  const permalink = getPathUrlAsString(
    getComponentSecurityHotspotsUrl(component.key, {
      ...getBranchLikeQuery(branchLike),
      hotspots: key,
    }),
    false
  );

  const categoryStandard = standards?.[SecurityStandard.SONARSOURCE][rule.securityCategory]?.title;

  return (
    <Header
      className="sw-sticky sw--mx-6 sw--mt-6 sw-px-6 sw-pt-6 sw-z-filterbar-header"
      style={{ top: `${LAYOUT_GLOBAL_NAV_HEIGHT + LAYOUT_PROJECT_NAV_HEIGHT - 2}px` }}
    >
      <div className="sw-flex sw-justify-between sw-gap-8 sw-mb-4 sw-pb-4">
        <div className="sw-flex-1">
          <StyledPageTitle as="h2" className="sw-whitespace-normal sw-overflow-visible">
            <LightPrimary>
              <IssueMessageHighlighting message={message} messageFormattings={messageFormattings} />
            </LightPrimary>
            <ClipboardIconButton
              Icon={LinkIcon}
              className="sw-ml-2"
              copyValue={permalink}
              discreet={true}
            />
          </StyledPageTitle>
          <div className="sw-mt-2 sw-mb-4 sw-body-sm">
            <LightLabel>{rule.name}</LightLabel>
            <Link className="sw-ml-1" to={getRuleUrl(rule.key)} target="_blank">
              {rule.key}
            </Link>
          </div>
          <Status
            hotspot={hotspot}
            onStatusChange={(statusOption) => props.onUpdateHotspot(true, statusOption)}
          />
        </div>
        <div className="sw-flex sw-flex-col sw-gap-4">
          <HotspotHeaderRightSection
            hotspot={hotspot}
            categoryStandard={categoryStandard}
            onUpdateHotspot={props.onUpdateHotspot}
          />
        </div>
      </div>
    </Header>
  );
}

const Header = withTheme(styled.div`
  background-color: ${themeColor('pageBlock')};
`);
