import React from 'react/addons';
import App from '../../src/main/js/apps/background-tasks/app';
import Header from '../../src/main/js/apps/background-tasks/header';
import {STATUSES, CURRENTS} from '../../src/main/js/apps/background-tasks/constants';

let TestUtils = React.addons.TestUtils;
let expect = require('chai').expect;

describe('Background Tasks', function () {
  describe('App', () => {
    it('should have #start()', () => {
      expect(App.start).to.be.a('function');
    });
  });

  describe('Constants', () => {
    it('should have STATUSES', () => {
      expect(STATUSES).to.be.a('object');
      expect(Object.keys(STATUSES).length).to.equal(6);
    });

    it('should have CURRENTS', () => {
      expect(CURRENTS).to.be.a('object');
      expect(Object.keys(CURRENTS).length).to.equal(2);
    });
  });

  describe('Header', () => {
    it('should render', () => {
      let component = TestUtils.renderIntoDocument(<Header/>),
          header = TestUtils.scryRenderedDOMComponentsWithTag(component, 'header');
      expect(header.length).to.equal(1);
    });
  });
});
