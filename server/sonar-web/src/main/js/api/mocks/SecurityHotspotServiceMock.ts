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
import { cloneDeep, times } from 'lodash';
import {
  mockHotspot,
  mockHotspotComment,
  mockHotspotRule,
  mockRawHotspot,
  mockStandards,
} from '../../helpers/mocks/security-hotspots';
import { mockSourceLine } from '../../helpers/mocks/sources';
import { getStandards } from '../../helpers/security-standard';
import { mockPaging, mockRestUser } from '../../helpers/testMocks';
import {
  Hotspot,
  HotspotAssignRequest,
  HotspotComment,
  HotspotResolution,
  HotspotStatus,
} from '../../types/security-hotspots';
import { RestUser } from '../../types/users';
import { getSources } from '../components';
import { getMeasures } from '../measures';
import {
  assignSecurityHotspot,
  commentSecurityHotspot,
  deleteSecurityHotspotComment,
  editSecurityHotspotComment,
  getSecurityHotspotDetails,
  getSecurityHotspotList,
  getSecurityHotspots,
  setSecurityHotspotStatus,
} from '../security-hotspots';
import { getUsers } from '../users';

const NUMBER_OF_LINES = 20;

export default class SecurityHotspotServiceMock {
  hotspots: Hotspot[] = [];
  nextAssignee: string | undefined;
  canChangeStatus: boolean = true;
  hotspotsComments: HotspotComment[] = [];

  constructor() {
    this.reset();

    jest.mocked(getMeasures).mockImplementation(this.handleGetMeasures);
    jest.mocked(getSecurityHotspots).mockImplementation(this.handleGetSecurityHotspots);
    jest.mocked(getSecurityHotspotDetails).mockImplementation(this.handleGetSecurityHotspotDetails);
    jest.mocked(getSecurityHotspotList).mockImplementation(this.handleGetSecurityHotspotList);
    jest.mocked(assignSecurityHotspot).mockImplementation(this.handleAssignSecurityHotspot);
    jest.mocked(setSecurityHotspotStatus).mockImplementation(this.handleSetSecurityHotspotStatus);
    jest.mocked(getUsers).mockImplementation((p) => this.handleGetUsers(p));
    jest.mocked(getSources).mockResolvedValue(
      times(NUMBER_OF_LINES, (n) =>
        mockSourceLine({
          line: n,
          code: '  <span class="sym-35 sym">symbole</span>',
        }),
      ),
    );
    jest.mocked(commentSecurityHotspot).mockImplementation(this.handleCommentSecurityHotspot);
    jest
      .mocked(deleteSecurityHotspotComment)
      .mockImplementation(this.handleDeleteSecurityHotspotComment);
    jest
      .mocked(editSecurityHotspotComment)
      .mockImplementation(this.handleEditSecurityHotspotComment);
    jest.mocked(getStandards).mockImplementation(this.handleGetStandards);
  }

  handleCommentSecurityHotspot = () => {
    this.hotspotsComments = [
      mockHotspotComment({
        htmlText: 'This is a comment from john doe',
        markdown: 'This is a comment from john doe',
        updatable: true,
      }),
    ];
    return Promise.resolve();
  };

  handleDeleteSecurityHotspotComment = () => {
    this.hotspotsComments = [];
    return Promise.resolve();
  };

  handleEditSecurityHotspotComment = () => {
    const response = mockHotspotComment({
      htmlText: 'This is a comment from john doe test',
      markdown: 'This is a comment from john doe test',
      updatable: true,
    });
    this.hotspotsComments = [response];
    return Promise.resolve(response);
  };

  handleGetStandards = () => {
    return Promise.resolve(mockStandards());
  };

  handleSetSecurityHotspotStatus = () => {
    return Promise.resolve();
  };

  handleGetUsers: typeof getUsers<RestUser> = () => {
    return this.reply({
      users: [
        mockRestUser({ name: 'User John', login: 'user.john' }),
        mockRestUser({ name: 'User Doe', login: 'user.doe' }),
        mockRestUser({ name: 'User Foo', login: 'user.foo' }),
      ],
      page: mockPaging(),
    });
  };

  handleGetSecurityHotspotList = (
    hotspotKeys: string[],
    data: {
      branch?: string;
      project: string;
    },
  ) => {
    if (data?.branch === 'normal-branch') {
      return this.reply({
        paging: mockPaging(),
        hotspots: [
          mockRawHotspot({
            assignee: 'John Doe',
            key: 'b1-test-1',
            message: "'F' is a magic number.",
          }),
          mockRawHotspot({ assignee: 'John Doe', key: 'b1-test-2' }),
        ].filter((h) => hotspotKeys.includes(h.key) || hotspotKeys.length === 0),
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
    }
    return this.reply({
      paging: mockPaging(),
      hotspots: this.mockRawHotspots(false).filter(
        (h) => hotspotKeys.includes(h.key) || hotspotKeys.length === 0,
      ),
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

  handleGetSecurityHotspots = (data: {
    branch?: string;
    inNewCodePeriod?: boolean;
    onlyMine?: boolean;
    p: number;
    project: string;
    ps: number;
    resolution?: HotspotResolution;
    status?: HotspotStatus;
  }) => {
    if (data?.branch === 'normal-branch') {
      return this.reply({
        paging: mockPaging({ pageIndex: 1, pageSize: data.ps, total: 2 }),
        hotspots: [
          mockRawHotspot({
            assignee: 'John Doe',
            key: 'b1-test-1',
            message: "'F' is a magic number.",
          }),
          mockRawHotspot({ assignee: 'John Doe', key: 'b1-test-2' }),
        ],
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
    }

    return this.reply({
      paging: mockPaging({ pageIndex: 1, pageSize: data.ps, total: this.hotspots.length }),
      hotspots: this.mockRawHotspots(data.onlyMine),
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

  mockRawHotspots = (onlyMine: boolean | undefined) => {
    if (onlyMine) {
      return [];
    }
    return [
      mockRawHotspot({ assignee: 'John Doe', key: 'test-1' }),
      mockRawHotspot({ assignee: 'John Doe', key: 'test-2' }),
      mockRawHotspot({ assignee: 'John Doe', key: 'test-cve', cveId: 'CVE-2021-12345' }),
    ];
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

    hotspot.canChangeStatus = this.canChangeStatus;
    hotspot.comment = this.hotspotsComments;

    return this.reply(hotspot);
  };

  handleGetMeasures = () => {
    return this.reply([
      {
        key: 'guillaume-peoch-sonarsource_benflix_AYGpXq2bd8qy4i0eO9ed',
        name: 'benflix',
        qualifier: 'TRK',
        metric: 'security_hotspots_reviewed',
        measures: [{ metric: 'security_hotspots_reviewed', value: '0.0', bestValue: false }],
      },
    ]);
  };

  handleAssignSecurityHotspot = (_: string, data: HotspotAssignRequest) => {
    this.nextAssignee = data.assignee;
    return Promise.resolve();
  };

  setHotspotChangeStatusPermission = (value: boolean) => (this.canChangeStatus = value);

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }

  reset = () => {
    this.hotspots = [
      mockHotspot({
        rule: mockHotspotRule({ key: 'rule2' }),
        assignee: 'John Doe',
        key: 'b1-test-1',
        message: "'F' is a magic number.",
      }),
      mockHotspot({
        rule: mockHotspotRule({ key: 'rule2' }),
        assignee: 'John Doe',
        key: 'b1-test-2',
      }),
      mockHotspot({
        rule: mockHotspotRule({ key: 'rule2' }),
        key: 'test-1',
        status: HotspotStatus.TO_REVIEW,
      }),
      mockHotspot({
        rule: mockHotspotRule({ key: 'rule2' }),
        key: 'test-2',
        status: HotspotStatus.TO_REVIEW,
        message: "'2' is a magic number.",
        codeVariants: ['variant 1', 'variant 2'],
      }),
      mockHotspot({
        rule: mockHotspotRule({ key: 'rule2' }),
        key: 'test-cve',
        status: HotspotStatus.TO_REVIEW,
        message: 'CVE on jackson',
        cveId: 'CVE-2021-12345',
      }),
    ];
    this.canChangeStatus = true;
  };
}
