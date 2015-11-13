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
  it('should display the zero Y tick if all values are zero', function () {
    let timeline = <Timeline width={100} height={100} data={ZERO_DATA} events={[]} formatYTick={FORMAT}/>;
    let output = TestUtils.renderIntoDocument(timeline);
    let tick = TestUtils.findRenderedDOMComponentWithClass(output, 'line-chart-tick-x');
    expect(tick.textContent).to.equal('0');
  });

  it('should display the zero Y tick if all values are undefined', function () {
    let timeline = <Timeline width={100} height={100} data={NULL_DATA} events={[]} formatYTick={FORMAT}/>;
    let output = TestUtils.renderIntoDocument(timeline);
    let tick = TestUtils.findRenderedDOMComponentWithClass(output, 'line-chart-tick-x');
    expect(tick.textContent).to.equal('0');
  });
});
