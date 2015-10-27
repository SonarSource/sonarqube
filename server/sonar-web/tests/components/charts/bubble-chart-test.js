import React from 'react';
import TestUtils from 'react-addons-test-utils';
import { expect } from 'chai';

import { BubbleChart } from '../../../src/main/js/components/charts/bubble-chart';


describe('Bubble Chart', function () {

  it('should display bubbles', function () {
    const items = [
      { x: 1, y: 10, size: 7 },
      { x: 2, y: 30, size: 5 },
      { x: 3, y: 20, size: 2 }
    ];
    let chart = TestUtils.renderIntoDocument(<BubbleChart items={items} width={100} height={100}/>);
    expect(TestUtils.scryRenderedDOMComponentsWithClass(chart, 'bubble-chart-bubble')).to.have.length(3);
  });

  it('should display grid', function () {
    const items = [
      { x: 1, y: 10, size: 7 },
      { x: 2, y: 30, size: 5 },
      { x: 3, y: 20, size: 2 }
    ];
    let chart = TestUtils.renderIntoDocument(<BubbleChart items={items} width={100} height={100}/>);
    expect(TestUtils.scryRenderedDOMComponentsWithTag(chart, 'line')).to.not.be.empty;
  });

  it('should display ticks', function () {
    const items = [
      { x: 1, y: 10, size: 7 },
      { x: 2, y: 30, size: 5 },
      { x: 3, y: 20, size: 2 }
    ];
    let chart = TestUtils.renderIntoDocument(<BubbleChart items={items} width={100} height={100}/>);
    expect(TestUtils.scryRenderedDOMComponentsWithClass(chart, 'bubble-chart-tick')).to.not.be.empty;
  });

});
