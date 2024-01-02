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
import { cloneDeep, uniqueId } from 'lodash';
import { mockAnalysis, mockAnalysisEvent } from '../../helpers/mocks/project-activity';
import { Analysis } from '../../types/project-activity';
import {
  changeEvent,
  createEvent,
  deleteAnalysis,
  deleteEvent,
  getProjectActivity,
} from '../projectActivity';

export class ProjectActivityServiceMock {
  readOnlyAnalysisList: Analysis[];
  analysisList: Analysis[];

  constructor(analyses?: Analysis[]) {
    this.readOnlyAnalysisList = analyses || [
      mockAnalysis({
        key: 'AXJMbIUGPAOIsUIE3eNT',
        date: '2017-03-03T09:36:01+0100',
        projectVersion: '1.1',
        buildString: '1.1.0.2',
        events: [
          mockAnalysisEvent({ category: 'VERSION', key: 'IsUIEAXJMbIUGPAO3eND', name: '1.1' }),
        ],
      }),
      mockAnalysis({
        key: 'AXJMbIUGPAOIsUIE3eND',
        date: '2017-03-02T09:36:01+0100',
        projectVersion: '1.1',
        buildString: '1.1.0.1',
      }),
      mockAnalysis({
        key: 'AXJMbIUGPAOIsUIE3eNE',
        date: '2017-03-01T10:36:01+0100',
        projectVersion: '1.0',
        buildString: '1.0.0.2',
        events: [
          mockAnalysisEvent({ category: 'VERSION', key: 'IUGPAOAXJMbIsUIE3eNE', name: '1.0' }),
        ],
      }),
      mockAnalysis({
        key: 'AXJMbIUGPAOIsUIE3eNC',
        date: '2017-03-01T09:36:01+0100',
        projectVersion: '1.0',
        buildString: '1.0.0.1',
      }),
    ];

    this.analysisList = cloneDeep(this.readOnlyAnalysisList);

    (getProjectActivity as jest.Mock).mockImplementation(this.getActivityHandler);
    (deleteAnalysis as jest.Mock).mockImplementation(this.deleteAnalysisHandler);
    (createEvent as jest.Mock).mockImplementation(this.createEventHandler);
    (changeEvent as jest.Mock).mockImplementation(this.changeEventHandler);
    (deleteEvent as jest.Mock).mockImplementation(this.deleteEventHandler);
  }

  reset = () => {
    this.analysisList = cloneDeep(this.readOnlyAnalysisList);
  };

  getActivityHandler = () => {
    return this.reply({
      analyses: this.analysisList,
      paging: {
        pageIndex: 1,
        pageSize: 100,
        total: this.analysisList.length,
      },
    });
  };

  deleteAnalysisHandler = (analysisKey: string) => {
    const i = this.analysisList.findIndex(({ key }) => key === analysisKey);
    if (i !== undefined) {
      this.analysisList.splice(i, 1);
      return this.reply();
    }
    throw new Error(`Could not find analysis with key: ${analysisKey}`);
  };

  createEventHandler = (
    analysisKey: string,
    name: string,
    category = 'OTHER',
    description?: string
  ) => {
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

  changeEventHandler = (eventKey: string, name: string, description?: string) => {
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

    return this.reply();
  };

  findEvent = (eventKey: string): [number, string] => {
    let analysisKey;
    const eventIndex = this.analysisList.reduce((acc, { key, events }) => {
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
    const analysis = this.analysisList.find(({ key }) => key === analysisKey);

    if (analysis !== undefined) {
      return analysis;
    }

    throw new Error(`Could not find analysis with key: ${analysisKey}`);
  };

  reply<T>(response?: T): Promise<T | void> {
    return Promise.resolve(response ? cloneDeep(response) : undefined);
  }
}
