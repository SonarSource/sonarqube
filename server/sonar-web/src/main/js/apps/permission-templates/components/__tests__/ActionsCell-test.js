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
import chai, { expect } from 'chai';
import sinon from 'sinon';
import sinonChai from 'sinon-chai';
import { shallow } from 'enzyme';
import React from 'react';
import ActionsCell from '../ActionsCell';

chai.use(sinonChai);

const SAMPLE = {
  id: 'id',
  name: 'name',
  permissions: [],
  defaultFor: []
};

function renderActionsCell (props) {
  return shallow(
      <ActionsCell
          permissionTemplate={SAMPLE}
          topQualifiers={['TRK', 'VW']}
          onUpdate={() => true}
          onDelete={() => true}
          refresh={() => true}
          {...props}/>
  );
}

function simulateClick (element) {
  element.simulate('click', {
    preventDefault() {}
  });
}

describe('Permission Templates :: ActionsCell', () => {
  it('should update', () => {
    const onUpdate = sinon.spy();
    const updateButton = renderActionsCell({ onUpdate }).find('.js-update');

    expect(updateButton).have.length(1);
    expect(onUpdate).to.not.have.been.called;

    simulateClick(updateButton);

    expect(onUpdate).to.have.been.called;
  });

  it('should delete', () => {
    const onDelete = sinon.spy();
    const deleteButton = renderActionsCell({ onDelete }).find('.js-delete');

    expect(deleteButton).have.length(1);
    expect(onDelete).to.not.have.been.called;

    simulateClick(deleteButton);

    expect(onDelete).to.have.been.called;
  });

  it('should not delete', () => {
    const permissionTemplate = { ...SAMPLE, defaultFor: ['VW'] };
    const deleteButton = renderActionsCell({ permissionTemplate })
        .find('.js-delete');

    expect(deleteButton).to.have.length(0);
  });

  it('should set default', () => {
    const setDefault = renderActionsCell()
        .find('.js-set-default');

    expect(setDefault).to.have.length(2);
    expect(setDefault.at(0).prop('data-qualifier')).to.equal('TRK');
    expect(setDefault.at(1).prop('data-qualifier')).to.equal('VW');
  });

  it('should not set default', () => {
    const permissionTemplate = { ...SAMPLE, defaultFor: ['TRK', 'VW'] };
    const setDefault = renderActionsCell({ permissionTemplate })
        .find('.js-set-default');

    expect(setDefault).to.have.length(0);
  });
});
