import { expect } from 'chai';
import React from 'react';
import TestUtils from 'react-addons-test-utils';

import { Timeline } from '../../../../src/main/js/apps/overview/components/timeline-chart';


const ZERO_DATA = [
  { x: new Date(2015, 0, 1), y: 0 },
  { x: new Date(2015, 0, 2), y: 0 },
  { x: new Date(2015, 0, 3), y: 0 },
  { x: new Date(2015, 0, 4), y: 0 }
];

const NULL_DATA = [
  { x: new Date(2015, 0, 1), y: null },
  { x: new Date(2015, 0, 2) },
  { x: new Date(2015, 0, 3), y: null },
  { x: new Date(2015, 0, 4) }
];

const FORMAT = (tick) => tick;


describe('TimelineChart', function () {
  it('should work with LEVEL', function () {
    const DATA = [
      { x: new Date(2015, 0, 1), y: 'OK' },
      { x: new Date(2015, 0, 2), y: 'WARN' },
      { x: new Date(2015, 0, 3), y: 'ERROR' },
      { x: new Date(2015, 0, 4), y: 'WARN' }
    ];

    let timeline = <Timeline width={100}
                             height={100}
                             data={DATA}
                             metricType="LEVEL"
                             events={[]}
                             formatValue={FORMAT}
                             formatYTick={FORMAT}/>;
    let output = TestUtils.renderIntoDocument(timeline);
    let ticks = TestUtils.scryRenderedDOMComponentsWithClass(output, 'line-chart-tick-x');
    expect(ticks).to.have.length(3);
    expect(ticks[0].textContent).to.equal('ERROR');
    expect(ticks[1].textContent).to.equal('WARN');
    expect(ticks[2].textContent).to.equal('OK');
  });

  it('should work with RATING', function () {
    const DATA = [
      { x: new Date(2015, 0, 1), y: 1 },
      { x: new Date(2015, 0, 2), y: 3 },
      { x: new Date(2015, 0, 3), y: 1 },
      { x: new Date(2015, 0, 4), y: 4 }
    ];

    let timeline = <Timeline width={100}
                             height={100}
                             data={DATA}
                             metricType="RATING"
                             events={[]}
                             formatValue={FORMAT}
                             formatYTick={FORMAT}/>;
    let output = TestUtils.renderIntoDocument(timeline);
    let ticks = TestUtils.scryRenderedDOMComponentsWithClass(output, 'line-chart-tick-x');
    expect(ticks).to.have.length(5);
    expect(ticks[0].textContent).to.equal('5');
    expect(ticks[1].textContent).to.equal('4');
    expect(ticks[2].textContent).to.equal('3');
    expect(ticks[3].textContent).to.equal('2');
    expect(ticks[4].textContent).to.equal('1');
  });

  it('should display the zero Y tick if all values are zero', function () {
    let timeline = <Timeline width={100}
                             height={100}
                             data={ZERO_DATA}
                             events={[]}
                             formatValue={FORMAT}
                             formatYTick={FORMAT}/>;
    let output = TestUtils.renderIntoDocument(timeline);
    let tick = TestUtils.findRenderedDOMComponentWithClass(output, 'line-chart-tick-x');
    expect(tick.textContent).to.equal('0');
  });

  it('should display the zero Y tick if all values are undefined', function () {
    let timeline = <Timeline width={100}
                             height={100}
                             data={NULL_DATA}
                             events={[]}
                             formatValue={FORMAT}
                             formatYTick={FORMAT}/>;
    let output = TestUtils.renderIntoDocument(timeline);
    let tick = TestUtils.findRenderedDOMComponentWithClass(output, 'line-chart-tick-x');
    expect(tick.textContent).to.equal('0');
  });
});
