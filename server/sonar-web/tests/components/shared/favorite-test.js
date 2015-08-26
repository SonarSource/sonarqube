var expect = require('chai').expect;

var React = require('react/addons');
var Favorite = require('../../../src/main/js/components/shared/favorite');
var TestUtils = React.addons.TestUtils;

describe('Favorite', function () {
  it('should render svg', function () {
    var favorite = TestUtils.renderIntoDocument(
        <Favorite component="id" favorite={false}/>
    );

    expect(TestUtils.findRenderedDOMComponentWithTag(favorite, 'svg')).to.be.ok;
    expect(TestUtils.findRenderedDOMComponentWithTag(favorite, 'path')).to.be.ok;
  });

  it('should render not favorite', function () {
    var favorite = TestUtils.renderIntoDocument(
        <Favorite component="id" favorite={false}/>
    );

    var link = TestUtils.findRenderedDOMComponentWithTag(favorite, 'a');
    expect(link.getDOMNode().className).to.equal('icon-star');
  });

  it('should render favorite', function () {
    var favorite = TestUtils.renderIntoDocument(
        <Favorite component="id" favorite={true}/>
    );

    var link = TestUtils.findRenderedDOMComponentWithTag(favorite, 'a');
    expect(link.getDOMNode().className).to.equal('icon-star icon-star-favorite');
  });
});
