import { expect } from 'chai';
import React from 'react';
import TestUtils from 'react-addons-test-utils';

import { Legend } from '../../../../src/main/js/apps/overview/components/legend';


const DATE = new Date(2015, 3, 7);
const LABEL = 'since 1.0';


describe('Legend', function () {
  it('should render', function () {
    let renderer = TestUtils.createRenderer();
    renderer.render(<Legend leakPeriodDate={DATE} leakPeriodLabel={LABEL}/>);
    let output = renderer.getRenderOutput();
    expect(output).to.not.be.null;
  });
});
