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
import { chunk, cloneDeep, uniqueId } from 'lodash';
import { BranchParameters } from '~sonar-aligned/types/branch-like';
import { parseDate } from '../../helpers/dates';
import { mockAnalysis, mockAnalysisEvent } from '../../helpers/mocks/project-activity';
import { Analysis, ProjectAnalysisEventCategory } from '../../types/project-activity';
import {
  changeEvent,
  createEvent,
  deleteAnalysis,
  deleteEvent,
  getAllTimeProjectActivity,
  getProjectActivity,
} from '../projectActivity';

jest.mock('../projectActivity');

const PAGE_SIZE = 10;
const DEFAULT_PAGE = 1;
const UNKNOWN_PROJECT = 'unknown';

const defaultAnalysesList = [
  mockAnalysis({
    key: 'AXJMbIUGPAOIsUIE3eNT',
    date: parseDate('2017-03-03T22:00:00.000Z').toDateString(),
    projectVersion: '1.1',
    buildString: '1.1.0.2',
    events: [
      mockAnalysisEvent({
        category: ProjectAnalysisEventCategory.Version,
        key: 'IsUIEAXJMbIUGPAO3eND',
        name: '1.1',
      }),
    ],
  }),
  mockAnalysis({
    key: 'AXJMbIUGPAOIsUIE3eND',
    date: parseDate('2017-03-02T22:00:00.000Z').toDateString(),
    projectVersion: '1.1',
    buildString: '1.1.0.1',
  }),
  mockAnalysis({
    key: 'AXJMbIUGPAOIsUIE3eNE',
    date: parseDate('2017-03-01T22:00:00.000Z').toDateString(),
    projectVersion: '1.0',
    events: [
      mockAnalysisEvent({
        category: ProjectAnalysisEventCategory.Version,
        key: 'IUGPAOAXJMbIsUIE3eNE',
        name: '1.0',
      }),
    ],
  }),
  mockAnalysis({
    key: 'AXJMbIUGPAOIsUIE3eNC',
    date: parseDate('2017-02-28T22:00:00.000Z').toDateString(),
    projectVersion: '1.0',
    buildString: '1.0.0.1',
  }),
];

export class ProjectActivityServiceMock {
  #analysisList: Analysis[];

  constructor() {
    this.#analysisList = cloneDeep(defaultAnalysesList);

    jest.mocked(getProjectActivity).mockImplementation(this.getActivityHandler);
    jest
      .mocked(getAllTimeProjectActivity)
      .mockImplementation(this.getAllTimeProjectActivityHandler);
    jest.mocked(deleteAnalysis).mockImplementation(this.deleteAnalysisHandler);
    jest.mocked(createEvent).mockImplementation(this.createEventHandler);
    jest.mocked(changeEvent).mockImplementation(this.changeEventHandler);
    jest.mocked(deleteEvent).mockImplementation(this.deleteEventHandler);
  }

  reset = () => {
    this.#analysisList = cloneDeep(defaultAnalysesList);
  };

  getAnalysesList = () => {
    return this.#analysisList;
  };

  setAnalysesList = (analyses: Analysis[]) => {
    this.#analysisList = analyses;
  };

  getActivityHandler = (
    data: {
      project: string;
      statuses?: string;
      category?: string;
      from?: string;
      p?: number;
      ps?: number;
    } & BranchParameters,
  ) => {
    const { project, ps = PAGE_SIZE, p = DEFAULT_PAGE, category, from } = data;

    if (project === UNKNOWN_PROJECT) {
      throw new Error(`Could not find project "${UNKNOWN_PROJECT}"`);
    }

    let analyses = category
      ? this.#analysisList.filter((a) => a.events.some((e) => e.category === category))
      : this.#analysisList;

    if (from !== undefined) {
      const fromTime = parseDate(from).getTime();
      analyses = analyses.filter((a) => parseDate(a.date).getTime() >= fromTime);
    }

    const analysesChunked = chunk(analyses, ps);

    return this.reply({
      paging: { pageSize: ps, total: analyses.length, pageIndex: p },
      analyses: analysesChunked[p - 1] ?? [],
    });
  };

  getAllTimeProjectActivityHandler = (
    data: {
      project: string;
      statuses?: string;
      category?: string;
      from?: string;
      p?: number;
      ps?: number;
    } & BranchParameters,
  ) => {
    const { project, p = DEFAULT_PAGE, category, from } = data;

    if (project === UNKNOWN_PROJECT) {
      throw new Error(`Could not find project "${UNKNOWN_PROJECT}"`);
    }

    let analyses = category
      ? this.#analysisList.filter((a) => a.events.some((e) => e.category === category))
      : this.#analysisList;

    if (from !== undefined) {
      const fromTime = parseDate(from).getTime();
      analyses = analyses.filter((a) => parseDate(a.date).getTime() >= fromTime);
    }
    return this.reply({
      paging: { pageSize: PAGE_SIZE, total: this.#analysisList.length, pageIndex: p },
      analyses: this.#analysisList,
    });
  };

  deleteAnalysisHandler = (analysisKey: string) => {
    const i = this.#analysisList.findIndex(({ key }) => key === analysisKey);
    if (i === undefined) {
      throw new Error(`Could not find analysis with key: ${analysisKey}`);
    }
    this.#analysisList.splice(i, 1);
    return this.reply(undefined);
  };

  createEventHandler = (data: {
    analysis: string;
    name: string;
    category?: ProjectAnalysisEventCategory;
    description?: string;
  }) => {
    const {
      analysis: analysisKey,
      name,
      category = ProjectAnalysisEventCategory.Other,
      description,
    } = data;
    const analysis = this.findAnalysis(analysisKey);

    const key = uniqueId(analysisKey);
    analysis.events.push({ key, name, category, description });

    return this.reply({
      analysis: analysisKey,
      key,
      name,
      category,
      description,
    });
  };

  changeEventHandler = (data: { event: string; name: string; description?: string }) => {
    const { event: eventKey, name, description } = data;
    const [eventIndex, analysisKey] = this.findEvent(eventKey);
    const analysis = this.findAnalysis(analysisKey);
    const event = analysis.events[eventIndex];

    event.name = name;
    event.description = description;

    return this.reply({ analysis: analysisKey, ...event });
  };

  deleteEventHandler = (eventKey: string) => {
    const [eventIndex, analysisKey] = this.findEvent(eventKey);
    const analysis = this.findAnalysis(analysisKey);

    analysis.events.splice(eventIndex, 1);

    return this.reply(undefined);
  };

  findEvent = (eventKey: string): [number, string] => {
    let analysisKey;
    const eventIndex = this.#analysisList.reduce((acc, { key, events }) => {
      if (acc === undefined) {
        const i = events.findIndex(({ key }) => key === eventKey);
        if (i > -1) {
          analysisKey = key;
          return i;
        }
      }

      return acc;
    }, undefined);

    if (eventIndex !== undefined && analysisKey !== undefined) {
      return [eventIndex, analysisKey];
    }

    throw new Error(`Could not find event with key: ${eventKey}`);
  };

  findAnalysis = (analysisKey: string) => {
    const analysis = this.#analysisList.find(({ key }) => key === analysisKey);

    if (analysis !== undefined) {
      return analysis;
    }

    throw new Error(`Could not find analysis with key: ${analysisKey}`);
  };

  reply<T>(response: T): Promise<T> {
    return Promise.resolve(cloneDeep(response));
  }
}
