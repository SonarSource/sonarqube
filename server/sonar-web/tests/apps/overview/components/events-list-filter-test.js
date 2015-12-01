import { expect } from 'chai';
import React from 'react';
import { findDOMNode } from 'react-dom';
import TestUtils from 'react-addons-test-utils';
import sinon from 'sinon';

import { EventsListFilter } from '../../../../src/main/js/apps/overview/components/events-list-filter';


describe('Overview :: EventsListFilter', function () {
  it('should render options', function () {
    let spy = sinon.spy();
    let output = TestUtils.renderIntoDocument(
        <EventsListFilter onFilter={spy} currentFilter="All"/>);
    let options = TestUtils.scryRenderedDOMComponentsWithTag(output, 'option');
    expect(options).to.have.length(5);
  });
});
