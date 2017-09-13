/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

// TODO Update type as new data are added

export type SysValueObject = { [name: string]: SysValue };
export type SysValue = boolean | string | number | SysValueObject;

interface ComputeEngine {
  pending: number;
  inProgress: number;
  waitingTimeMs: number;
  workers: number;
  workersPerNode: number;
}

interface Database {
  name: string;
  version: string;
}

interface Settings {
  'sonar.forceAuthentication': boolean;
}

export enum HealthType {
  RED = 'RED',
  YELLOW = 'YELLOW',
  GREEN = 'GREEN'
}

export interface HealthCause {
  message: string;
}

export interface ApplicationNode {
  name: string;
  host: string;
  health: HealthType;
  healthCauses: HealthCause[];
  startedAt: number;
  officialDistribution: string;
  processors: number;
}

export interface SearchNode {
  name: string;
  host: string;
  health: HealthType;
  healthCauses: HealthCause[];
  startedAt: number;
  processors: number;
}

export interface SysInfo {
  cluster: boolean;
  clusterName: string;
  serverId: string;
  health: HealthType;
  healthCauses: HealthCause[];
  settings: Settings;
  computeEngine: ComputeEngine;
  database: Database;
  applicationNodes: ApplicationNode[];
  searchNodes: SearchNode[];
}
