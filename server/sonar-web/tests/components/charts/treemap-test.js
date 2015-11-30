import React from 'react';
import TestUtils from 'react-addons-test-utils';
import { expect } from 'chai';

import { Treemap } from '../../../src/main/js/components/charts/treemap';


describe('Treemap', function () {

  it('should display', function () {
    const items = [
      { size: 10, color: '#777', label: 'SonarQube :: Server' },
      { size: 30, color: '#777', label: 'SonarQube :: Web' },
      { size: 20, color: '#777', label: 'SonarQube :: Search' }
    ];
    let chart = TestUtils.renderIntoDocument(
        <Treemap items={items} width={100} height={100} breadcrumbs={[]} canBeClicked={() => true}/>);
    expect(TestUtils.scryRenderedDOMComponentsWithClass(chart, 'treemap-cell')).to.have.length(3);
  });

});
