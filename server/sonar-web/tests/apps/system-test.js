import React from 'react';
import ReactDOM from 'react-dom';
import TestUtils from 'react-addons-test-utils';

import ItemValue from '../../src/main/js/apps/system/item-value';

let expect = require('chai').expect;
let sinon = require('sinon');

describe('System', function () {

  describe('Item Value', function () {
    it('should render string', () => {
      let result = TestUtils.renderIntoDocument(<ItemValue value="/some/path/as/an/example"/>);
      let content = ReactDOM.findDOMNode(TestUtils.findRenderedDOMComponentWithTag(result, 'code'));
      expect(content.textContent).to.equal('/some/path/as/an/example');
    });

    it('should render `true`', () => {
      let result = TestUtils.renderIntoDocument(<ItemValue value={true}/>);
      TestUtils.findRenderedDOMComponentWithClass(result, 'icon-check');
    });

    it('should render `false`', () => {
      let result = TestUtils.renderIntoDocument(<ItemValue value={false}/>);
      TestUtils.findRenderedDOMComponentWithClass(result, 'icon-delete');
    });

    it('should render object', () => {
      let result = TestUtils.renderIntoDocument(<ItemValue value={{ name: 'Java', version: '3.2' }}/>);
      TestUtils.findRenderedDOMComponentWithTag(result, 'table');
      expect(TestUtils.scryRenderedDOMComponentsWithTag(result, 'tr')).to.have.length(2);
    });

    it('should render `true` inside object', () => {
      let result = TestUtils.renderIntoDocument(<ItemValue value={{ name: 'Java', isCool: true }}/>);
      TestUtils.findRenderedDOMComponentWithTag(result, 'table');
      TestUtils.findRenderedDOMComponentWithClass(result, 'icon-check');
    });

    it('should render object inside object', () => {
      let result = TestUtils.renderIntoDocument(
          <ItemValue value={{ users: { docs: 1, shards: 5 }, tests: { docs: 68, shards: 5 }  }}/>);
      expect(TestUtils.scryRenderedDOMComponentsWithTag(result, 'table')).to.have.length(3);
      expect(TestUtils.scryRenderedDOMComponentsWithTag(result, 'tr')).to.have.length(6);
    });
  });

  describe('Log Level', function () {
    var previousFetch, fetchUrl, fetchOptions;

    before(function () {
      previousFetch = window.fetch;
      window.fetch = function (url, options) {
        fetchUrl = url;
        fetchOptions = options;
        return Promise.resolve();
      };
    });

    after(function () {
      window.fetch = previousFetch;
    });

    it('should render select box', () => {
      let result = TestUtils.renderIntoDocument(<ItemValue value="INFO" name="Logs Level"/>);
      TestUtils.findRenderedDOMComponentWithTag(result, 'select');
      expect(TestUtils.scryRenderedDOMComponentsWithTag(result, 'option')).to.have.length(3);
    });

    it('should set initial value', () => {
      let result = TestUtils.renderIntoDocument(<ItemValue value="DEBUG" name="Logs Level"/>);
      let select = ReactDOM.findDOMNode(TestUtils.findRenderedDOMComponentWithTag(result, 'select'));
      expect(select.value).to.equal('DEBUG');
    });

    it('should render warning', () => {
      let result = TestUtils.renderIntoDocument(<ItemValue value="DEBUG" name="Logs Level"/>);
      TestUtils.findRenderedDOMComponentWithClass(result, 'alert');
    });

    it('should not render warning', () => {
      let result = TestUtils.renderIntoDocument(<ItemValue value="INFO" name="Logs Level"/>);
      expect(TestUtils.scryRenderedDOMComponentsWithClass(result, 'alert')).to.be.empty;
    });

    it('should change value', () => {
      let result = TestUtils.renderIntoDocument(<ItemValue value="INFO" name="Logs Level"/>);
      let select = ReactDOM.findDOMNode(TestUtils.findRenderedDOMComponentWithTag(result, 'select'));
      select.value = 'TRACE';
      TestUtils.Simulate.change(select);
      expect(fetchUrl).to.equal('/api/system/change_log_level');
      expect(fetchOptions.method).to.equal('POST');
      expect(fetchOptions.body).to.equal('level=TRACE');
    });
  });

});
