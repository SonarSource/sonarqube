import _ from 'underscore';
import { expect } from 'chai';
import React from 'react';
import TestUtils from 'react-addons-test-utils';

import { GeneralCoverage } from '../../../../src/main/js/apps/overview/main/coverage';


const COMPONENT = { key: 'component-key' };

const DATE = new Date(2015, 0, 1);

const MEASURES = {
  'overall_coverage': 73.5,
  'coverage': 69.7,
  'it_coverage': 54.0,
  'tests': 137
};
const LEAK = {
  'new_overall_coverage': 72.5,
  'new_coverage': 68.7,
  'new_it_coverage': 53.0
};
const MEASURES_FOR_UT = _.omit(MEASURES, 'overall_coverage');
const LEAK_FOR_UT = _.omit(LEAK, 'new_overall_coverage');
const MEASURES_FOR_IT = _.omit(MEASURES_FOR_UT, 'coverage');
const LEAK_FOR_IT = _.omit(LEAK_FOR_UT, 'new_coverage');


describe('Overview :: GeneralCoverage', function () {
  it('should display tests', function () {
    let component = <GeneralCoverage measures={MEASURES} component={COMPONENT} coverageMetricPrefix=""/>;
    let output = TestUtils.renderIntoDocument(component);
    let coverageElement = TestUtils.findRenderedDOMComponentWithClass(output, 'js-overview-main-tests');
    expect(coverageElement.textContent).to.equal('137');
  });

  it('should not display tests', function () {
    let measuresWithoutTests = _.omit(MEASURES, 'tests');
    let component = <GeneralCoverage measures={measuresWithoutTests} component={COMPONENT} coverageMetricPrefix=""/>;
    let output = TestUtils.renderIntoDocument(component);
    let coverageElements = TestUtils.scryRenderedDOMComponentsWithClass(output, 'js-overview-main-tests');
    expect(coverageElements).to.be.empty;
  });

  it('should fall back to UT coverage', function () {
    let component = <GeneralCoverage measures={MEASURES_FOR_UT} leak={LEAK_FOR_UT} component={COMPONENT}
                                     leakPeriodDate={DATE} coverageMetricPrefix=""/>;
    let output = TestUtils.renderIntoDocument(component);

    let coverageElement = TestUtils.findRenderedDOMComponentWithClass(output, 'js-overview-main-coverage');
    expect(coverageElement.textContent).to.equal('69.7%');

    let newCoverageElement = TestUtils.findRenderedDOMComponentWithClass(output, 'js-overview-main-new-coverage');
    expect(newCoverageElement.textContent).to.equal('68.7%');
  });

  it('should fall back to IT coverage', function () {
    let component = <GeneralCoverage measures={MEASURES_FOR_IT} leak={LEAK_FOR_IT} component={COMPONENT}
                                     leakPeriodDate={DATE} coverageMetricPrefix="it_"/>;
    let output = TestUtils.renderIntoDocument(component);

    let coverageElement = TestUtils.findRenderedDOMComponentWithClass(output, 'js-overview-main-coverage');
    expect(coverageElement.textContent).to.equal('54.0%');

    let newCoverageElement = TestUtils.findRenderedDOMComponentWithClass(output, 'js-overview-main-new-coverage');
    expect(newCoverageElement.textContent).to.equal('53.0%');
  });
});
