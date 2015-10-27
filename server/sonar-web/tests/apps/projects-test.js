import React from 'react';
import TestUtils from 'react-addons-test-utils';

import Projects from '../../src/main/js/apps/projects/projects';

let expect = require('chai').expect;
let sinon = require('sinon');

describe('Projects', function () {
  describe('Projects', () => {
    it('should render list of projects with no selection', () => {
      let projects = [
        { id: '1', key: 'a', name: 'A', qualifier: 'TRK' },
        { id: '2', key: 'b', name: 'B', qualifier: 'TRK' }
      ];

      let result = TestUtils.renderIntoDocument(
          <Projects projects={projects} selection={[]} refresh={sinon.spy()}/>);
      expect(TestUtils.scryRenderedDOMComponentsWithTag(result, 'tr')).to.have.length(2);
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'icon-checkbox-checked')).to.be.empty;
    });

    it('should render list of projects with one selected', () => {
      let projects = [
            { id: '1', key: 'a', name: 'A', qualifier: 'TRK' },
            { id: '2', key: 'b', name: 'B', qualifier: 'TRK' }
          ],
          selection = ['1'];

      let result = TestUtils.renderIntoDocument(
          <Projects projects={projects} selection={selection} refresh={sinon.spy()}/>);
      expect(TestUtils.scryRenderedDOMComponentsWithTag(result, 'tr')).to.have.length(2);
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'icon-checkbox-checked')).to.have.length(1);
    });
  });
});
