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
import { getJSON, post } from 'sonar-ui-common/helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';
import {
  DetailedHotspot,
  HotspotSearchResponse,
  HotspotSetStatusRequest
} from '../types/security-hotspots';

export function setSecurityHotspotStatus(data: HotspotSetStatusRequest): Promise<void> {
  return post('/api/hotspots/change_status', data).catch(throwGlobalError);
}

export function getSecurityHotspots(data: {
  projectKey: string;
  p: number;
  ps: number;
}): Promise<HotspotSearchResponse> {
  return getJSON('/api/hotspots/search', data).catch(throwGlobalError);
}

export function getSecurityHotspotDetails(securityHotspotKey: string): Promise<DetailedHotspot> {
  return getJSON('/api/hotspots/show', { hotspot: securityHotspotKey }).catch(throwGlobalError);
}
