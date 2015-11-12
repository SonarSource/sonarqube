import React from 'react';
import ReactDOM from 'react-dom';
import TestUtils from 'react-addons-test-utils';
import { expect } from 'chai';

import Gate from '../../../src/main/js/apps/overview/gate/gate';
import GateConditions from '../../../src/main/js/apps/overview/gate/gate-conditions';
import GateCondition from '../../../src/main/js/apps/overview/gate/gate-condition';

describe('Overview', function () {

  describe('Quality Gate', function () {
    it('should display a badge', function () {
      let output = TestUtils.renderIntoDocument(<Gate gate={{ level: 'ERROR', conditions: [] }} component={{ }}/>);
      TestUtils.findRenderedDOMComponentWithClass(output, 'badge-error');
    });

    it('should not be displayed', function () {
      let output = TestUtils.renderIntoDocument(<Gate component={{ }}/>);
      expect(TestUtils.scryRenderedDOMComponentsWithClass(output, 'overview-gate')).to.be.empty;
    });

    it('should display empty gate', function () {
      let output = TestUtils.renderIntoDocument(<Gate component={{ qualifier: 'TRK' }}/>);
      TestUtils.findRenderedDOMComponentWithClass(output, 'overview-gate');
      TestUtils.findRenderedDOMComponentWithClass(output, 'overview-gate-warning');
    });

    it('should filter out passed conditions', function () {
      const conditions = [
        { level: 'OK' },
        { level: 'ERROR', metric: { name: 'error metric' } },
        { level: 'WARN', metric: { name: 'warn metric' } },
        { level: 'OK' }
      ];

      let renderer = TestUtils.createRenderer();
      renderer.render(<GateConditions gate={{ conditions }} component={{}}/>);
      let output = renderer.getRenderOutput();
      expect(output.props.children).to.have.length(2);
    });
  });


  describe('Helpers', function () {
    describe('Periods', function () {

    });
  });

});
