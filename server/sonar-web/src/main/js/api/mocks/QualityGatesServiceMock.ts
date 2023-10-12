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
import { cloneDeep, flatten, omit, remove } from 'lodash';
import { Project } from '../../apps/quality-gates/components/Projects';
import {
  mockQualityGate,
  mockQualityGateApplicationStatus,
  mockQualityGateProjectStatus,
} from '../../helpers/mocks/quality-gates';
import { mockUserBase } from '../../helpers/mocks/users';
import { mockCondition, mockGroup } from '../../helpers/testMocks';
import { MetricKey } from '../../types/metrics';
import {
  QualityGateApplicationStatus,
  QualityGateProjectStatus,
  SearchPermissionsParameters,
} from '../../types/quality-gates';
import { CaycStatus, Condition, QualityGate } from '../../types/types';
import {
  addGroup,
  addUser,
  associateGateWithProject,
  copyQualityGate,
  createCondition,
  createQualityGate,
  deleteCondition,
  deleteQualityGate,
  dissociateGateWithProject,
  fetchQualityGate,
  fetchQualityGates,
  getApplicationQualityGate,
  getGateForProject,
  getQualityGateProjectStatus,
  renameQualityGate,
  searchGroups,
  searchProjects,
  searchUsers,
  setQualityGateAsDefault,
  updateCondition,
} from '../quality-gates';

jest.mock('../quality-gates');

export class QualityGatesServiceMock {
  isAdmin = false;
  readOnlyList: QualityGate[];
  list: QualityGate[];
  projects: Project[];
  getGateForProjectGateName: string;
  throwOnGetGateForProject: boolean;
  qualityGateProjectStatus: QualityGateProjectStatus;
  applicationQualityGate: QualityGateApplicationStatus;

  constructor(list?: QualityGate[]) {
    this.readOnlyList = list || [
      mockQualityGate({
        name: 'SonarSource way',
        conditions: [
          {
            id: 'AXJMbIUGPAOIsUIE3eNC',
            metric: 'new_coverage',
            op: 'LT',
            error: '85',
            isCaycCondition: true,
          },
          {
            id: 'AXJMbIUGPAOIsUIE3eNF',
            metric: 'new_violations',
            op: 'GT',
            error: '0',
            isCaycCondition: true,
          },
          { id: 'AXJMbIUGPAOIsUIE3eNE', metric: 'reliability_rating', op: 'GT', error: '4' },
          { id: 'AXJMbIUGPAOIsUIE3eND', metric: 'security_rating', op: 'GT', error: '4' },
          {
            id: 'AXJMbIUGPAOIsUIE3eNT',
            metric: 'new_maintainability_rating',
            op: 'GT',
            error: '1',
          },
          { id: 'AXJMbIUGPAOIsUIE3eNU', metric: 'new_reliability_rating', op: 'GT', error: '1' },
          { id: 'AXJMbIUGPAOIsUIE3eNV', metric: 'new_security_rating', op: 'GT', error: '1' },
          {
            id: 'AXJMbIUHPAOIsUIE3eNc',
            metric: 'new_duplicated_lines_density',
            op: 'GT',
            error: '3',
            isCaycCondition: true,
          },
          {
            id: 'AXJMbIUHPAOIsUIE3eOi',
            metric: 'new_security_hotspots_reviewed',
            op: 'LT',
            error: '100',
            isCaycCondition: true,
          },
          {
            id: 'AXJMbIUHPAOIsUIE3eNfc',
            metric: MetricKey.new_line_coverage,
            op: 'GT',
            error: '3',
          },
        ],
        isDefault: true,
        isBuiltIn: false,
        caycStatus: CaycStatus.Compliant,
      }),
      mockQualityGate({
        name: 'SonarSource way - CFamily',
        conditions: [
          {
            id: 'AXJMbIUHPAOIsUIE3eOu',
            metric: 'new_coverage',
            op: 'LT',
            error: '0',
            isCaycCondition: true,
          },
          { id: 'AXJMbIUHPAOIsUIE3eNs', metric: 'new_security_rating', op: 'GT', error: '1' },
          { id: 'AXJMbIUHPAOIsUIE3eNs', metric: 'new_security_rating', op: 'GT', error: '0' },
          { id: 'deprecated', metric: 'function_complexity', op: 'LT', error: '1' },
        ],
        isDefault: false,
        isBuiltIn: false,
        caycStatus: CaycStatus.NonCompliant,
      }),
      mockQualityGate({
        name: 'Sonar way',
        conditions: [
          {
            id: 'AXJMbIUHPAOIsUIE3eNs',
            metric: 'new_violations',
            op: 'GT',
            error: '0',
            isCaycCondition: true,
          },
          {
            id: 'AXJMbIUHPAOIsUIE3eOF',
            metric: 'new_coverage',
            op: 'LT',
            error: '80',
            isCaycCondition: true,
          },
          {
            id: 'AXJMbIUHPAOIsUIE3eOG',
            metric: 'new_duplicated_lines_density',
            op: 'GT',
            error: '3',
            isCaycCondition: true,
          },
          {
            id: 'AXJMbIUHPAOIsUIE3eOk',
            metric: 'new_security_hotspots_reviewed',
            op: 'LT',
            error: '100',
            isCaycCondition: true,
          },
        ],
        isDefault: false,
        isBuiltIn: true,
        caycStatus: CaycStatus.Compliant,
      }),
      mockQualityGate({
        name: 'Non Cayc QG',
        conditions: [
          { id: 'AXJMbIUHPAOIsUIE3eNs', metric: 'new_security_rating', op: 'GT', error: '1' },
          { id: 'AXJMbIUHPAOIsUIE3eOD', metric: 'new_reliability_rating', op: 'GT', error: '1' },
          { id: 'AXJMbIUHPAOIsUIE3eOF', metric: 'new_coverage', op: 'LT', error: '80' },
        ],
        isDefault: false,
        isBuiltIn: false,
        caycStatus: CaycStatus.NonCompliant,
      }),
      mockQualityGate({
        name: 'Over Compliant CAYC QG',
        conditions: [
          { id: 'deprecatedoc', metric: 'function_complexity', op: 'LT', error: '1' },
          { id: 'AXJMbIUHPAOIsUIE3eOFoc', metric: 'new_coverage', op: 'LT', error: '80' },
          { id: 'AXJMbIUHPAOIsUIE3eNsoc', metric: 'new_security_rating', op: 'GT', error: '1' },
          { id: 'AXJMbIUHPAOIsUIE3eODoc', metric: 'new_reliability_rating', op: 'GT', error: '1' },
          {
            id: 'AXJMbIUHPAOIsUIE3eOEoc',
            metric: 'new_maintainability_rating',
            op: 'GT',
            error: '1',
          },
          { id: 'AXJMbIUHPAOIsUIE3eOFocdl', metric: 'new_coverage', op: 'LT', error: '80' },
          {
            id: 'AXJMbIUHPAOIsUIE3eOGoc',
            metric: 'new_duplicated_lines_density',
            op: 'GT',
            error: '3',
          },
          {
            id: 'AXJMbIUHPAOIsUIE3eOkoc',
            metric: 'new_security_hotspots_reviewed',
            op: 'LT',
            error: '100',
          },
        ],
        isDefault: false,
        isBuiltIn: false,
        caycStatus: CaycStatus.OverCompliant,
      }),
      mockQualityGate({
        name: 'QG without conditions',
        conditions: [],
        isDefault: false,
        isBuiltIn: false,
        caycStatus: CaycStatus.NonCompliant,
      }),
      mockQualityGate({
        name: 'QG without new code conditions',
        conditions: [
          { id: 'AXJMbIUHPAOIsUIE3eNs', metric: 'security_rating', op: 'GT', error: '1' },
        ],
        isDefault: false,
        isBuiltIn: false,
        caycStatus: CaycStatus.NonCompliant,
      }),
    ];

    this.list = cloneDeep(this.readOnlyList);

    this.projects = [
      { key: 'test1', name: 'test1', selected: false },
      { key: 'test2', name: 'test2', selected: false },
      { key: 'test3', name: 'test3', selected: true },
      { key: 'test4', name: 'test4', selected: true },
    ];

    this.getGateForProjectGateName = 'SonarSource way';
    this.throwOnGetGateForProject = false;

    jest.mocked(fetchQualityGate).mockImplementation(this.showHandler);
    jest.mocked(fetchQualityGates).mockImplementation(this.listHandler);
    jest.mocked(createQualityGate).mockImplementation(this.createHandler);
    jest.mocked(deleteQualityGate).mockImplementation(this.destroyHandler);
    jest.mocked(copyQualityGate).mockImplementation(this.copyHandler);
    (renameQualityGate as jest.Mock).mockImplementation(this.renameHandler);
    jest.mocked(createCondition).mockImplementation(this.createConditionHandler);
    jest.mocked(updateCondition).mockImplementation(this.updateConditionHandler);
    jest.mocked(deleteCondition).mockImplementation(this.deleteConditionHandler);
    jest.mocked(searchProjects).mockImplementation(this.searchProjectsHandler);
    jest.mocked(searchUsers).mockImplementation(this.searchUsersHandler);
    jest.mocked(searchGroups).mockImplementation(this.searchGroupsHandler);
    jest.mocked(associateGateWithProject).mockImplementation(this.selectHandler);
    jest.mocked(dissociateGateWithProject).mockImplementation(this.deSelectHandler);
    jest.mocked(setQualityGateAsDefault).mockImplementation(this.setDefaultHandler);
    (getGateForProject as jest.Mock).mockImplementation(this.projectGateHandler);
    jest.mocked(getQualityGateProjectStatus).mockImplementation(this.handleQualityGetProjectStatus);
    jest.mocked(getApplicationQualityGate).mockImplementation(this.handleGetApplicationQualityGate);

    this.qualityGateProjectStatus = mockQualityGateProjectStatus({});
    this.applicationQualityGate = mockQualityGateApplicationStatus({});

    // To be implemented.
    (addUser as jest.Mock).mockResolvedValue({});
    (addGroup as jest.Mock).mockResolvedValue({});
  }

  getCorruptedQualityGateName() {
    return 'SonarSource way - CFamily';
  }

  reset() {
    this.setIsAdmin(false);
    this.list = cloneDeep(this.readOnlyList);
    this.getGateForProjectGateName = 'SonarSource way';
  }

  getDefaultQualityGate() {
    return this.list.find((q) => q.isDefault) || mockQualityGate({ isDefault: true });
  }

  getBuiltInQualityGate() {
    return this.list.find((q) => q.isBuiltIn) || mockQualityGate({ isBuiltIn: true });
  }

  setIsAdmin(isAdmin: boolean) {
    this.isAdmin = isAdmin;
  }

  setGetGateForProjectName(name: string) {
    this.getGateForProjectGateName = name;
  }

  setThrowOnGetGateForProject(value: boolean) {
    this.throwOnGetGateForProject = value;
  }

  computeActions(q: QualityGate) {
    return {
      rename: q.isBuiltIn ? false : this.isAdmin,
      setAsDefault: q.isDefault ? false : this.isAdmin,
      copy: this.isAdmin,
      associateProjects: this.isAdmin,
      delete: q.isBuiltIn ? false : this.isAdmin,
      manageConditions: this.isAdmin,
      delegate: this.isAdmin,
    };
  }

  listHandler = () => {
    return this.reply({
      qualitygates: this.list
        .map((q) => omit(q, 'conditions'))
        .map((q) => ({
          ...q,
          actions: this.computeActions(q),
        })),
      default: this.getDefaultQualityGate().name,
      actions: { create: this.isAdmin },
    });
  };

  showHandler = ({ name }: { name: string }) => {
    const qualityGate = omit(
      this.list.find((q) => q.name === name),
      'isDefault',
    );
    return this.reply({ ...qualityGate, actions: this.computeActions(qualityGate) });
  };

  createHandler = ({ name }: { name: string }) => {
    this.list.push(
      mockQualityGate({
        name,
        conditions: [
          mockCondition({
            id: `${MetricKey.new_reliability_rating}1`,
            metric: MetricKey.new_reliability_rating,
            error: '1',
          }),
          mockCondition({
            id: `${MetricKey.new_maintainability_rating}1`,
            metric: MetricKey.new_maintainability_rating,
            error: '1',
          }),
          mockCondition({
            id: `${MetricKey.new_security_rating}1`,
            metric: MetricKey.new_security_rating,
            error: '1',
          }),
          mockCondition({
            id: `${MetricKey.new_security_hotspots_reviewed}1`,
            metric: MetricKey.new_security_hotspots_reviewed,
            error: '100',
          }),
        ],
        isDefault: false,
        isBuiltIn: false,
        caycStatus: CaycStatus.Compliant,
      }),
    );
    return this.reply({
      name,
    });
  };

  destroyHandler = ({ name }: { name: string }) => {
    this.list = this.list.filter((q) => q.name !== name);
    return Promise.resolve();
  };

  copyHandler = ({ sourceName, name }: { sourceName: string; name: string }) => {
    const newQG = cloneDeep(this.list.find((q) => q.name === sourceName));
    if (newQG === undefined) {
      return Promise.reject({
        errors: [{ msg: `No quality gate has been found for name ${sourceName}` }],
      });
    }
    newQG.name = name;

    newQG.isDefault = false;
    newQG.isBuiltIn = false;

    this.list.push(newQG);

    return this.reply({
      name,
    });
  };

  renameHandler = ({ currentName, name }: { currentName: string; name: string }) => {
    const renameQG = this.list.find((q) => q.name === currentName);
    if (renameQG === undefined) {
      return Promise.reject({
        errors: [{ msg: `No quality gate has been found for name ${currentName}` }],
      });
    }
    renameQG.name = name;
    return this.reply({
      name,
    });
  };

  setDefaultHandler = ({ name }: { name: string }) => {
    this.list.forEach((q) => {
      q.isDefault = false;
    });
    const selectedQG = this.list.find((q) => q.name === name);
    if (selectedQG === undefined) {
      return Promise.reject({
        errors: [{ msg: `No quality gate has been found for name ${name}` }],
      });
    }
    selectedQG.isDefault = true;
    return Promise.resolve();
  };

  createConditionHandler = (
    data: {
      gateName: string;
    } & Omit<Condition, 'id'>,
  ) => {
    const { metric, gateName, op, error, isCaycCondition } = data;
    const qg = this.list.find((q) => q.name === gateName);
    if (qg === undefined) {
      return Promise.reject({
        errors: [{ msg: `No quality gate has been found for name ${gateName}` }],
      });
    }

    const conditions = qg.conditions || [];
    const id = `condId${qg.name}${conditions.length}`;
    const newCondition = { metric, op, error, id, isCaycCondition };

    conditions.push(newCondition);
    qg.conditions = conditions;
    return this.reply(newCondition);
  };

  updateConditionHandler = ({ id, metric, op, error, isCaycCondition }: Condition) => {
    const condition = flatten(this.list.map((q) => q.conditions || [])).find((q) => q.id === id);
    if (condition === undefined) {
      return Promise.reject({ errors: [{ msg: `No condition has been found for id ${id}` }] });
    }

    condition.metric = metric;
    condition.op = op;
    condition.error = error;
    condition.isCaycCondition = isCaycCondition;

    return this.reply(condition);
  };

  deleteConditionHandler = ({ id }: { id: string }) => {
    this.list.forEach((q) => {
      remove(q.conditions || [], (c) => c.id === id);
    });
    return Promise.resolve();
  };

  searchProjectsHandler = ({
    selected,
    query,
  }: {
    gateName: string;
    selected: string;
    query: string | undefined;
  }) => {
    let filteredProjects = this.projects;
    if (selected === 'selected') {
      filteredProjects = this.projects.filter((p) => p.selected);
    } else if (selected === 'deselected') {
      filteredProjects = this.projects.filter((p) => !p.selected);
    }

    if (query !== '' && query !== undefined) {
      filteredProjects = filteredProjects.filter((p) => p.name.includes(query));
    }

    const response = {
      paging: { pageIndex: 1, pageSize: 3, total: 55 },
      results: filteredProjects,
    };
    return this.reply(response);
  };

  searchUsersHandler = ({ selected }: SearchPermissionsParameters) => {
    if (selected === 'selected') {
      return this.reply({ users: [] });
    }

    return this.reply({ users: [mockUserBase()] });
  };

  searchGroupsHandler = ({ selected }: SearchPermissionsParameters) => {
    if (selected === 'selected') {
      return this.reply({ groups: [] });
    }

    return this.reply({ groups: [mockGroup()] });
  };

  selectHandler = ({ projectKey }: { projectKey: string }) => {
    const changedProject = this.projects.find((p) => p.key === projectKey);
    if (changedProject) {
      changedProject.selected = true;
    }
    return Promise.resolve();
  };

  deSelectHandler = ({ projectKey }: { projectKey: string }) => {
    const changedProject = this.projects.find((p) => p.key === projectKey);
    if (changedProject) {
      changedProject.selected = false;
    }
    return Promise.resolve();
  };

  projectGateHandler = () => {
    if (this.throwOnGetGateForProject) {
      return Promise.reject('unknown');
    }

    return this.reply(this.list.find((qg) => qg.name === this.getGateForProjectGateName));
  };

  handleGetApplicationQualityGate = () => {
    return this.reply(this.applicationQualityGate);
  };

  setApplicationQualityGateStatus = (status: QualityGateApplicationStatus) => {
    this.applicationQualityGate = mockQualityGateApplicationStatus(status);
  };

  handleQualityGetProjectStatus = () => {
    return this.reply(this.qualityGateProjectStatus);
  };

  setQualityGateProjectStatus = (status: QualityGateProjectStatus) => {
    this.qualityGateProjectStatus = mockQualityGateProjectStatus(status);
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
