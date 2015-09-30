import React from 'react/addons';
import App from '../../src/main/js/apps/background-tasks/app';
import Header from '../../src/main/js/apps/background-tasks/header';
import Search from '../../src/main/js/apps/background-tasks/search';
import {STATUSES, CURRENTS, DEBOUNCE_DELAY} from '../../src/main/js/apps/background-tasks/constants';

let TestUtils = React.addons.TestUtils;
let chai = require('chai');
let expect = chai.expect;
let sinon = require('sinon');
chai.use(require('sinon-chai'));

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

  describe('Search', () => {
    it('should render search form', () => {
      let spy = sinon.spy();
      let component = TestUtils.renderIntoDocument(<Search options={{}}
                                                           onStatusChange={spy}
                                                           onCurrentsChange={spy}
                                                           onDateChange={spy}/>),
          searchBox = TestUtils.scryRenderedDOMComponentsWithClass(component, 'search-box');
      expect(searchBox).to.have.length(1);
    });

    it('should not render search form', () => {
      let spy = sinon.spy();
      let component = TestUtils.renderIntoDocument(<Search options={{ componentId: 'ABCD' }}
                                                           onStatusChange={spy}
                                                           onCurrentsChange={spy}
                                                           onDateChange={spy}/>),
          searchBox = TestUtils.scryRenderedDOMComponentsWithClass(component, 'search-box');
      expect(searchBox).to.be.empty;
    });

    it('should search', (done) => {
      let spy = sinon.spy(),
          searchSpy = sinon.spy();
      let component = TestUtils.renderIntoDocument(<Search options={{}}
                                                           onStatusChange={spy}
                                                           onCurrentsChange={spy}
                                                           onDateChange={spy}
                                                           onSearch={searchSpy}/>);
      let searchInput = React.findDOMNode(TestUtils.findRenderedDOMComponentWithClass(component, 'search-box-input'));
      searchInput.value = 'some search query';
      TestUtils.Simulate.change(searchInput);
      setTimeout(() => {
        expect(searchSpy).to.have.been.calledWith('some search query');
        done();
      }, DEBOUNCE_DELAY);
    });
  });
});
