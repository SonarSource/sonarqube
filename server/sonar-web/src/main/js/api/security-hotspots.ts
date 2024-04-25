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
import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { getJSON } from '~sonar-aligned/helpers/request';
import { post } from '../helpers/request';
import { BranchParameters } from '../types/branch-like';
import {
  Hotspot,
  HotspotAssignRequest,
  HotspotComment,
  HotspotResolution,
  HotspotSearchResponse,
  HotspotSetStatusRequest,
  HotspotStatus,
} from '../types/security-hotspots';
import { UserBase } from '../types/users';

const HOTSPOTS_LIST_URL = '/api/hotspots/list';
const HOTSPOTS_SEARCH_URL = '/api/hotspots/search';

export function assignSecurityHotspot(
  hotspotKey: string,
  data: HotspotAssignRequest,
): Promise<void> {
  return post('/api/hotspots/assign', { hotspot: hotspotKey, ...data }).catch(throwGlobalError);
}

export function setSecurityHotspotStatus(
  hotspotKey: string,
  data: HotspotSetStatusRequest,
): Promise<void> {
  return post('/api/hotspots/change_status', { hotspot: hotspotKey, ...data }).catch(
    throwGlobalError,
  );
}

export function commentSecurityHotspot(hotspotKey: string, comment: string): Promise<void> {
  return post('/api/hotspots/add_comment', { hotspot: hotspotKey, comment }).catch(
    throwGlobalError,
  );
}

export function deleteSecurityHotspotComment(commentKey: string): Promise<void> {
  return post('/api/hotspots/delete_comment', { comment: commentKey }).catch(throwGlobalError);
}

export function editSecurityHotspotComment(
  commentKey: string,
  comment: string,
): Promise<HotspotComment> {
  return post('/api/hotspots/edit_comment', { comment: commentKey, text: comment }).catch(
    throwGlobalError,
  );
}

export function getSecurityHotspots(
  data: {
    inNewCodePeriod?: boolean;
    onlyMine?: boolean;
    p: number;
    project: string;
    ps: number;
    resolution?: HotspotResolution;
    status?: HotspotStatus;
  } & BranchParameters,
  projectIsIndexing = false,
): Promise<HotspotSearchResponse> {
  return getJSON(projectIsIndexing ? HOTSPOTS_LIST_URL : HOTSPOTS_SEARCH_URL, data).catch(
    throwGlobalError,
  );
}

export function getSecurityHotspotList(
  hotspotKeys: string[],
  data: {
    project: string;
  } & BranchParameters,
  projectIsIndexing = false,
): Promise<HotspotSearchResponse> {
  return getJSON(projectIsIndexing ? HOTSPOTS_LIST_URL : HOTSPOTS_SEARCH_URL, {
    ...data,
    hotspots: hotspotKeys.join(),
  }).catch(throwGlobalError);
}

export function getSecurityHotspotDetails(securityHotspotKey: string): Promise<Hotspot> {
  return getJSON('/api/hotspots/show', { hotspot: securityHotspotKey })
    .then((response: Hotspot & { users: UserBase[] }) => {
      const { users, ...hotspot } = response;

      if (users) {
        if (hotspot.assignee) {
          hotspot.assigneeUser = users.find((u) => u.login === hotspot.assignee) || {
            active: true,
            login: hotspot.assignee,
          };
        }

        hotspot.authorUser = users.find((u) => u.login === hotspot.author) || {
          active: true,
          login: hotspot.author,
        };

        hotspot.comment.forEach((c) => {
          c.user = users.find((u) => u.login === c.login) || { active: true, login: c.login };
        });
      }

      return hotspot;
    })
    .catch(throwGlobalError);
}
