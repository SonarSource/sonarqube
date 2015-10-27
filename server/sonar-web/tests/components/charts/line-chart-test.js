import React from 'react';
import TestUtils from 'react-addons-test-utils';
import { expect } from 'chai';

import { LineChart } from '../../../src/main/js/components/charts/line-chart';


describe('Line Chart', function () {

  it('should display line', function () {
    const data = [
      { x: 1, y: 10 },
      { x: 2, y: 30 },
      { x: 3, y: 20 }
    ];
    let chart = TestUtils.renderIntoDocument(<LineChart data={data} width={100} height={100}/>);
    expect(TestUtils.scryRenderedDOMComponentsWithClass(chart, 'line-chart-path')).to.have.length(1);
  });

  it('should display ticks', function () {
    const data = [
      { x: 1, y: 10 },
      { x: 2, y: 30 },
      { x: 3, y: 20 }
    ];
    const ticks = ['A', 'B', 'C'];
    let chart = TestUtils.renderIntoDocument(<LineChart data={data} xTicks={ticks} width={100} height={100}/>);
    expect(TestUtils.scryRenderedDOMComponentsWithClass(chart, 'line-chart-tick')).to.have.length(3);
  });

  it('should display values', function () {
    const data = [
      { x: 1, y: 10 },
      { x: 2, y: 30 },
      { x: 3, y: 20 }
    ];
    const values = ['A', 'B', 'C'];
    let chart = TestUtils.renderIntoDocument(<LineChart data={data} xValues={values} width={100} height={100}/>);
    expect(TestUtils.scryRenderedDOMComponentsWithClass(chart, 'line-chart-tick')).to.have.length(3);
  });

});
