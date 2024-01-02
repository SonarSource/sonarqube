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
import { cloneDeep, flatten, omit, remove } from 'lodash';
import { Project } from '../../apps/quality-gates/components/Projects';
import { mockQualityGate } from '../../helpers/mocks/quality-gates';
import { mockUserBase } from '../../helpers/mocks/users';
import { mockCondition, mockGroup } from '../../helpers/testMocks';
import { MetricKey } from '../../types/metrics';
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
  renameQualityGate,
  searchGroups,
  searchProjects,
  searchUsers,
  setQualityGateAsDefault,
  updateCondition,
} from '../quality-gates';

export class QualityGatesServiceMock {
  isAdmin = false;
  readOnlyList: QualityGate[];
  list: QualityGate[];
  projects: Project[];

  constructor(list?: QualityGate[], defaultId = 'AWBWEMe2qGAMGEYPjJlm') {
    this.readOnlyList = list || [
      mockQualityGate({
        id: defaultId,
        name: 'SonarSource way',
        conditions: [
          { id: 'AXJMbIUGPAOIsUIE3eNC', metric: 'new_coverage', op: 'LT', error: '85' },
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
          },
          {
            id: 'AXJMbIUHPAOIsUIE3eOi',
            metric: 'new_security_hotspots_reviewed',
            op: 'LT',
            error: '100',
          },
        ],
        isDefault: true,
        isBuiltIn: false,
        caycStatus: CaycStatus.Compliant,
      }),
      mockQualityGate({
        id: 'AXGYZrDqC-YjVCvvbRDY',
        name: 'SonarSource way - CFamily',
        conditions: [
          { id: 'AXJMbIUHPAOIsUIE3eOu', metric: 'new_coverage', op: 'LT', error: '0' },
          { id: 'AXJMbIUHPAOIsUIE3eOubis', metric: 'new_coverage', op: 'LT', error: '1' },
          { id: 'deprecated', metric: 'function_complexity', op: 'LT', error: '1' },
        ],
        isDefault: false,
        isBuiltIn: false,
        caycStatus: CaycStatus.NonCompliant,
      }),
      mockQualityGate({
        id: 'AWBWEMe4qGAMGEYPjJlr',
        name: 'Sonar way',
        conditions: [
          { id: 'AXJMbIUHPAOIsUIE3eNs', metric: 'new_security_rating', op: 'GT', error: '1' },
          { id: 'AXJMbIUHPAOIsUIE3eOD', metric: 'new_reliability_rating', op: 'GT', error: '1' },
          {
            id: 'AXJMbIUHPAOIsUIE3eOE',
            metric: 'new_maintainability_rating',
            op: 'GT',
            error: '1',
          },
          { id: 'AXJMbIUHPAOIsUIE3eOF', metric: 'new_coverage', op: 'LT', error: '80' },
          {
            id: 'AXJMbIUHPAOIsUIE3eOG',
            metric: 'new_duplicated_lines_density',
            op: 'GT',
            error: '3',
          },
          {
            id: 'AXJMbIUHPAOIsUIE3eOk',
            metric: 'new_security_hotspots_reviewed',
            op: 'LT',
            error: '100',
          },
        ],
        isDefault: false,
        isBuiltIn: true,
        caycStatus: CaycStatus.Compliant,
      }),
      mockQualityGate({
        id: 'AWBWEMe4qGAMGEYPjJlruit',
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
        id: 'AWBWEMe4qGAMGEYPjJlruitoc',
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
    ];

    this.list = cloneDeep(this.readOnlyList);

    this.projects = [
      { key: 'test1', name: 'test1', selected: false },
      { key: 'test2', name: 'test2', selected: false },
      { key: 'test3', name: 'test3', selected: true },
      { key: 'test4', name: 'test4', selected: true },
    ];

    (fetchQualityGate as jest.Mock).mockImplementation(this.showHandler);
    (fetchQualityGates as jest.Mock).mockImplementation(this.listHandler);
    (createQualityGate as jest.Mock).mockImplementation(this.createHandler);
    (deleteQualityGate as jest.Mock).mockImplementation(this.destroyHandler);
    (copyQualityGate as jest.Mock).mockImplementation(this.copyHandler);
    (renameQualityGate as jest.Mock).mockImplementation(this.renameHandler);
    (createCondition as jest.Mock).mockImplementation(this.createConditionHandler);
    (updateCondition as jest.Mock).mockImplementation(this.updateConditionHandler);
    (deleteCondition as jest.Mock).mockImplementation(this.deleteConditionHandler);
    (searchProjects as jest.Mock).mockImplementation(this.searchProjectsHandler);
    (searchUsers as jest.Mock).mockImplementation(this.searchUsersHandler);
    (searchGroups as jest.Mock).mockImplementation(this.searchGroupsHandler);
    (associateGateWithProject as jest.Mock).mockImplementation(this.selectHandler);
    (dissociateGateWithProject as jest.Mock).mockImplementation(this.deSelectHandler);
    (setQualityGateAsDefault as jest.Mock).mockImplementation(this.setDefaultHandler);

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
      default: this.getDefaultQualityGate().id,
      actions: { create: this.isAdmin },
    });
  };

  showHandler = ({ id }: { id: string }) => {
    const qualityGate = omit(
      this.list.find((q) => q.id === id),
      'isDefault'
    );
    return this.reply({ ...qualityGate, actions: this.computeActions(qualityGate) });
  };

  createHandler = ({ name }: { name: string }) => {
    const newId = `newId${this.list.length}`;
    this.list.push(
      mockQualityGate({
        id: newId,
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
      })
    );
    return this.reply({
      id: newId,
      name,
    });
  };

  destroyHandler = ({ id }: { id: string }) => {
    this.list = this.list.filter((q) => q.id !== id);
    return Promise.resolve();
  };

  copyHandler = ({ id, name }: { id: string; name: string }) => {
    const newQG = cloneDeep(this.list.find((q) => q.id === id));
    if (newQG === undefined) {
      return Promise.reject({ errors: [{ msg: `No quality gate has been found for id ${id}` }] });
    }
    newQG.name = name;
    newQG.id = `newId${this.list.length}`;

    newQG.isDefault = false;
    newQG.isBuiltIn = false;

    this.list.push(newQG);

    return this.reply({
      id: newQG.id,
      name,
    });
  };

  renameHandler = ({ id, name }: { id: string; name: string }) => {
    const renameQG = this.list.find((q) => q.id === id);
    if (renameQG === undefined) {
      return Promise.reject({ errors: [{ msg: `No quality gate has been found for id ${id}` }] });
    }
    renameQG.name = name;
    return this.reply({
      id: renameQG.id,
      name,
    });
  };

  setDefaultHandler = ({ id }: { id: string }) => {
    this.list.forEach((q) => {
      q.isDefault = false;
    });
    const selectedQG = this.list.find((q) => q.id === id);
    if (selectedQG === undefined) {
      return Promise.reject({ errors: [{ msg: `No quality gate has been found for id ${id}` }] });
    }
    selectedQG.isDefault = true;
    return Promise.resolve();
  };

  createConditionHandler = (
    data: {
      gateId: string;
    } & Omit<Condition, 'id'>
  ) => {
    const { metric, gateId, op, error } = data;
    const qg = this.list.find((q) => q.id === gateId);
    if (qg === undefined) {
      return Promise.reject({
        errors: [{ msg: `No quality gate has been found for id ${gateId}` }],
      });
    }

    const conditions = qg.conditions || [];
    const id = `condId${qg.id}${conditions.length}`;
    const newCondition = { id, metric, op, error };
    conditions.push(newCondition);
    qg.conditions = conditions;
    return this.reply(newCondition);
  };

  updateConditionHandler = ({ id, metric, op, error }: Condition) => {
    const condition = flatten(this.list.map((q) => q.conditions || [])).find((q) => q.id === id);
    if (condition === undefined) {
      return Promise.reject({ errors: [{ msg: `No condition has been found for id ${id}` }] });
    }

    condition.metric = metric;
    condition.op = op;
    condition.error = error;

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

  searchUsersHandler = ({ selected }: { selected: string }) => {
    if (selected === 'selected') {
      return this.reply({ users: [] });
    }

    return this.reply({ users: [mockUserBase()] });
  };

  searchGroupsHandler = ({ selected }: { selected: string }) => {
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

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
