import { expect } from 'chai';
import React from 'react';
import { findDOMNode } from 'react-dom';
import TestUtils from 'react-addons-test-utils';

import { Event } from '../../../../src/main/js/apps/overview/components/event';


describe('Overview :: Event', function () {
  it('should render event', function () {
    let output = TestUtils.renderIntoDocument(
        <Event event={{ id: '1', name: '1.5', type: 'Version', date: new Date(2015, 0, 1) }}/>);
    expect(
        findDOMNode(TestUtils.findRenderedDOMComponentWithClass(output, 'js-event-date')).textContent
    ).to.include('2015');
    expect(
        findDOMNode(TestUtils.findRenderedDOMComponentWithClass(output, 'js-event-name')).textContent
    ).to.include('1.5');
    expect(
        findDOMNode(TestUtils.findRenderedDOMComponentWithClass(output, 'js-event-type')).textContent
    ).to.include('Version');
  });
});
