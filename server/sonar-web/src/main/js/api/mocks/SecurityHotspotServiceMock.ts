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
import { cloneDeep, pick, range, times } from 'lodash';
import { mockHotspot, mockRawHotspot } from '../../helpers/mocks/security-hotspots';
import { mockSourceLine } from '../../helpers/mocks/sources';
import { mockRuleDetails } from '../../helpers/testMocks';
import { BranchParameters } from '../../types/branch-like';
import {
  Hotspot,
  HotspotAssignRequest,
  HotspotResolution,
  HotspotStatus,
} from '../../types/security-hotspots';
import { getSources } from '../components';
import { getMeasures } from '../measures';
import { getRuleDetails } from '../rules';
import {
  assignSecurityHotspot,
  getSecurityHotspotDetails,
  getSecurityHotspots,
} from '../security-hotspots';

const NUMBER_OF_LINES = 20;
const MAX_END_RANGE = 10;

export default class SecurityHotspotServiceMock {
  hotspots: Hotspot[] = [];
  rawHotspotKey: string[] = [];
  nextAssignee: string | undefined;

  constructor() {
    this.reset();

    (getMeasures as jest.Mock).mockImplementation(this.handleGetMeasures);
    (getSecurityHotspots as jest.Mock).mockImplementation(this.handleGetSecurityHotspots);
    (getSecurityHotspotDetails as jest.Mock).mockImplementation(
      this.handleGetSecurityHotspotDetails
    );
    (assignSecurityHotspot as jest.Mock).mockImplementation(this.handleAssignSecurityHotspot);
    (getRuleDetails as jest.Mock).mockResolvedValue({ rule: mockRuleDetails() });
    (getSources as jest.Mock).mockResolvedValue(
      times(NUMBER_OF_LINES, (n) =>
        mockSourceLine({
          line: n,
          code: '  <span class="sym-35 sym">symbole</span>',
        })
      )
    );
  }

  handleGetSources = (data: { key: string; from?: number; to?: number } & BranchParameters) => {
    return this.reply(
      range(data.from || 1, data.to || MAX_END_RANGE).map((line) => mockSourceLine({ line }))
    );
  };

  handleGetSecurityHotspots = (
    data: {
      projectKey: string;
      p: number;
      ps: number;
      status?: HotspotStatus;
      resolution?: HotspotResolution;
      onlyMine?: boolean;
      inNewCodePeriod?: boolean;
    } & BranchParameters
  ) => {
    return this.reply({
      paging: { pageIndex: 1, pageSize: data.ps, total: this.hotspots.length },
      hotspots: this.hotspots.map((hotspot) => pick(hotspot, this.rawHotspotKey)),
      components: [
        {
          key: 'guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed:index.php',
          qualifier: 'FIL',
          name: 'index.php',
          longName: 'index.php',
          path: 'index.php',
        },
        {
          key: 'guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed',
          qualifier: 'TRK',
          name: 'benflix',
          longName: 'benflix',
        },
      ],
    });
  };

  handleGetSecurityHotspotDetails = (securityHotspotKey: string) => {
    const hotspot = this.hotspots.find((h) => h.key === securityHotspotKey);

    if (hotspot === undefined) {
      return Promise.reject({
        errors: [{ msg: `No security hotspot for key ${securityHotspotKey}` }],
      });
    }

    if (this.nextAssignee !== undefined) {
      hotspot.assigneeUser = {
        ...hotspot.assigneeUser,
        login: this.nextAssignee,
        name: this.nextAssignee,
      };
      this.nextAssignee = undefined;
    }

    return this.reply(hotspot);
  };

  handleGetMeasures = () => {
    return this.reply([
      {
        component: {
          key: 'guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed',
          name: 'benflix',
          qualifier: 'TRK',
          measures: [{ metric: 'security_hotspots_reviewed', value: '0.0', bestValue: false }],
        },
      },
    ]);
  };

  handleAssignSecurityHotspot = (_: string, data: HotspotAssignRequest) => {
    this.nextAssignee = data.assignee;
    return this.reply({});
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }

  reset = () => {
    this.rawHotspotKey = Object.keys(mockRawHotspot());
    this.hotspots = [
      mockHotspot({ key: '1', status: HotspotStatus.TO_REVIEW }),
      mockHotspot({ key: '2', status: HotspotStatus.TO_REVIEW }),
    ];
  };
}
