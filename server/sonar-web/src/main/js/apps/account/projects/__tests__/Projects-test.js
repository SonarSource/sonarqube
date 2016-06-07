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
import React from 'react';
import { shallow } from 'enzyme';
import { expect } from 'chai';
import Projects from '../Projects';
import ProjectCard from '../ProjectCard';
import ListFooter from '../../../../components/shared/list-footer';

describe('My Account :: Projects', () => {
  it('should render list of ProjectCards', () => {
    const projects = [
      { id: 'id1', key: 'key1', name: 'name1', links: [] },
      { id: 'id2', key: 'key2', name: 'name2', links: [] }
    ];

    const output = shallow(
        <Projects
            projects={projects}
            total={0}
            loading={false}
            loadMore={() => true}/>
    );

    expect(output.find(ProjectCard)).to.have.length(2);
  });

  it('should render ListFooter', () => {
    const output = shallow(
        <Projects
            projects={[]}
            total={0}
            loading={false}
            loadMore={() => true}/>
    );

    expect(output.find(ListFooter)).to.have.length(1);
  });
});
