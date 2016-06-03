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
import ProjectCard from '../ProjectCard';
import Level from '../../../../components/ui/Level';

const BASE = { id: 'id', key: 'key', name: 'name', links: [] };

describe('My Account :: ProjectCard', () => {
  it('should render key and name', () => {
    const project = { ...BASE };
    const output = shallow(
        <ProjectCard project={project}/>
    );
    expect(output.find('.account-project-key').text()).to.equal('key');
    expect(output.find('.account-project-name').text()).to.equal('name');
  });

  it('should render description', () => {
    const project = { ...BASE, description: 'bla' };
    const output = shallow(
        <ProjectCard project={project}/>
    );
    expect(output.find('.account-project-description').text()).to.equal('bla');
  });

  it('should not render optional fields', () => {
    const project = { ...BASE };
    const output = shallow(
        <ProjectCard project={project}/>
    );
    expect(output.find('.account-project-description')).to.have.length(0);
    expect(output.find('.account-project-quality-gate')).to.have.length(0);
    expect(output.find('.account-project-links')).to.have.length(0);
  });

  it('should render analysis date', () => {
    const project = { ...BASE, lastAnalysisDate: '2016-05-17' };
    const output = shallow(
        <ProjectCard project={project}/>
    );
    expect(output.find('.account-project-analysis').text())
        .to.contain('my_account.projects.analyzed_x');
  });

  it('should not render analysis date', () => {
    const project = { ...BASE };
    const output = shallow(
        <ProjectCard project={project}/>
    );
    expect(output.find('.account-project-analysis').text())
        .to.contain('my_account.projects.never_analyzed');
  });

  it('should render quality gate status', () => {
    const project = { ...BASE, qualityGate: 'ERROR' };
    const output = shallow(
        <ProjectCard project={project}/>
    );
    expect(
        output.find('.account-project-quality-gate').find(Level).prop('level')
    ).to.equal('ERROR');
  });

  it('should render links', () => {
    const project = {
      ...BASE,
      links: [{ name: 'n', type: 't', href: 'h' }]
    };
    const output = shallow(
        <ProjectCard project={project}/>
    );
    expect(output.find('.account-project-links').find('li')).to.have.length(1);
  });
});
