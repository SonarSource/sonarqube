import { expect } from 'chai';
import React from 'react';
import TestUtils from 'react-addons-test-utils';

import { LanguageDistribution } from '../../../../src/main/js/apps/overview/components/language-distribution';


const DISTRIBUTION = '<null>=17345;java=194342;js=20984';
const LINES = 1000000;


describe('LanguageDistribution', function () {
  let props;

  beforeEach(function () {
    let renderer = TestUtils.createRenderer();
    renderer.render(<LanguageDistribution distribution={DISTRIBUTION} lines={LINES}/>);
    let output = renderer.getRenderOutput();
    let child = React.Children.only(output.props.children);
    props = child.props;
  });

  it('should pass right data', function () {
    expect(props.data).to.deep.equal([
      { x: 194342, y: 1, value: 'java' },
      { x: 20984, y: 2, value: 'js' },
      { x: 17345, y: 0, value: '<null>' }
    ]);
  });

  it('should pass right yTicks', function () {
    expect(props.yTicks).to.deep.equal(['java', 'js', '<null>']);
  });

  it('should pass right yValues', function () {
    expect(props.yValues).to.deep.equal(['19.4%', '2.1%', '1.7%']);
  });
});
