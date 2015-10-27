import React from 'react';
import TestUtils from 'react-addons-test-utils';
import { expect } from 'chai';

import { BarChart } from '../../../src/main/js/components/charts/bar-chart';


describe('Bar Chart', function () {

  it('should display bars', function () {
    const data = [
      { x: 1, y: 10 },
      { x: 2, y: 30 },
      { x: 3, y: 20 }
    ];
    let chart = TestUtils.renderIntoDocument(<BarChart data={data} width={100} height={100}/>);
    expect(TestUtils.scryRenderedDOMComponentsWithClass(chart, 'bar-chart-bar')).to.have.length(3);
  });

  it('should display ticks', function () {
    const data = [
      { x: 1, y: 10 },
      { x: 2, y: 30 },
      { x: 3, y: 20 }
    ];
    const ticks = ['A', 'B', 'C'];
    let chart = TestUtils.renderIntoDocument(<BarChart data={data} xTicks={ticks} width={100} height={100}/>);
    expect(TestUtils.scryRenderedDOMComponentsWithClass(chart, 'bar-chart-tick')).to.have.length(3);
  });

  it('should display values', function () {
    const data = [
      { x: 1, y: 10 },
      { x: 2, y: 30 },
      { x: 3, y: 20 }
    ];
    const values = ['A', 'B', 'C'];
    let chart = TestUtils.renderIntoDocument(<BarChart data={data} xValues={values} width={100} height={100}/>);
    expect(TestUtils.scryRenderedDOMComponentsWithClass(chart, 'bar-chart-tick')).to.have.length(3);
  });

  it('should display bars, ticks and values', function () {
    const data = [
      { x: 1, y: 10 },
      { x: 2, y: 30 },
      { x: 3, y: 20 }
    ];
    const ticks = ['A', 'B', 'C'];
    const values = ['A', 'B', 'C'];
    let chart = TestUtils.renderIntoDocument(
        <BarChart data={data} xTicks={ticks} xValues={values} width={100} height={100}/>);
    expect(TestUtils.scryRenderedDOMComponentsWithClass(chart, 'bar-chart-bar')).to.have.length(3);
    expect(TestUtils.scryRenderedDOMComponentsWithClass(chart, 'bar-chart-tick')).to.have.length(6);
  });

});
