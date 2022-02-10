/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import styled from '@emotion/styled';
import React from 'react';
import { colors, sizes } from '../../../app/theme';
import { ClipboardButton, ClipboardIconButton } from '../../../components/controls/clipboard';
import { withCurrentUser } from '../../../components/hoc/withCurrentUser';
import LinkIcon from '../../../components/icons/LinkIcon';
import QualifierIcon from '../../../components/icons/QualifierIcon';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { collapsedDirFromPath, fileFromPath } from '../../../helpers/path';
import { getComponentSecurityHotspotsUrl, getPathUrlAsString } from '../../../helpers/urls';
import { isLoggedIn } from '../../../helpers/users';
import { BranchLike } from '../../../types/branch-like';
import { ComponentQualifier } from '../../../types/component';
import { Hotspot } from '../../../types/security-hotspots';
import { Component, CurrentUser } from '../../../types/types';
import HotspotOpenInIdeButton from './HotspotOpenInIdeButton';

export interface HotspotSnippetHeaderProps {
  hotspot: Hotspot;
  currentUser: CurrentUser;
  component: Component;
  branchLike?: BranchLike;
}

export function HotspotSnippetHeader(props: HotspotSnippetHeaderProps) {
  const { hotspot, currentUser, component, branchLike } = props;
  const {
    project,
    component: { qualifier, path }
  } = hotspot;

  const displayProjectName = component.qualifier === ComponentQualifier.Application;

  const permalink = getPathUrlAsString(
    getComponentSecurityHotspotsUrl(component.key, {
      ...getBranchLikeQuery(branchLike),
      hotspots: hotspot?.key
    }),
    false
  );

  return (
    <Container>
      <div className="display-flex-row">
        <FilePath>
          {displayProjectName && (
            <>
              <QualifierIcon
                className="little-spacer-right"
                qualifier={ComponentQualifier.Project}
              />
              <span className="little-spacer-right">{project.name}</span>
            </>
          )}
          <QualifierIcon qualifier={qualifier} /> <span>{collapsedDirFromPath(path)}</span>
          <span className="little-spacer-right">{fileFromPath(path)}</span>
          <ClipboardIconButton className="button-link link-no-underline" copyValue={path} />
        </FilePath>

        {isLoggedIn(currentUser) && (
          <div className="dropdown spacer-right">
            <HotspotOpenInIdeButton hotspotKey={hotspot.key} projectKey={project.key} />
          </div>
        )}
      </div>

      <ClipboardButton copyValue={permalink}>
        <span>{translate('hotspots.get_permalink')}</span>
        <LinkIcon className="spacer-left" />
      </ClipboardButton>
    </Container>
  );
}

const Container = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 40px;
  padding: 0 ${sizes.gridSize};
  border: 1px solid ${colors.barBorderColor};
  background-color: ${colors.barBackgroundColor};
`;

const FilePath = styled.div`
  display: flex;
  align-items: center;
  margin-right: 40px;
  color: ${colors.secondFontColor};
  svg {
    margin: 0 ${sizes.gridSize};
  }
`;

export default withCurrentUser(HotspotSnippetHeader);
