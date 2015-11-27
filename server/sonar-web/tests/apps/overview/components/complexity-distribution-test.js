import { expect } from 'chai';
import React from 'react';
import TestUtils from 'react-addons-test-utils';

import { ComplexityDistribution } from '../../../../src/main/js/apps/overview/components/complexity-distribution';


const DISTRIBUTION = '1=11950;2=86;4=77;6=43;8=17;10=12;12=3';


describe('ComplexityDistribution', function () {
  let props;

  beforeEach(function () {
    let renderer = TestUtils.createRenderer();
    renderer.render(<ComplexityDistribution distribution={DISTRIBUTION} of="function"/>);
    let output = renderer.getRenderOutput();
    let child = React.Children.only(output.props.children);
    props = child.props;
  });

  it('should pass right data', function () {
    expect(props.data).to.deep.equal([
      { x: 0, y: 11950, value: 1, tooltip: 'overview.complexity_tooltip.function.11950.1' },
      { x: 1, y: 86, value: 2, tooltip: 'overview.complexity_tooltip.function.86.2' },
      { x: 2, y: 77, value: 4, tooltip: 'overview.complexity_tooltip.function.77.4' },
      { x: 3, y: 43, value: 6, tooltip: 'overview.complexity_tooltip.function.43.6' },
      { x: 4, y: 17, value: 8, tooltip: 'overview.complexity_tooltip.function.17.8' },
      { x: 5, y: 12, value: 10, tooltip: 'overview.complexity_tooltip.function.12.10' },
      { x: 6, y: 3, value: 12, tooltip: 'overview.complexity_tooltip.function.3.12' }
    ]);
  });

  it('should pass right xTicks', function () {
    expect(props.xTicks).to.deep.equal([1, 2, 4, 6, 8, 10, 12]);
  });

  it('should pass right xValues', function () {
    expect(props.xValues).to.deep.equal(['11,950', '86', '77', '43', '17', '12', '3']);
  });
});
