import helper from '../../src/main/js/components/source-viewer/helpers/code-with-issue-locations-helper';

let expect = require('chai').expect;

describe('Source Viewer', function () {
  describe('Code With Issue Locations Helper', function () {
    it('should be a function', function () {
      expect(helper).to.be.a('function');
    });

    it('should mark one location', function () {
      var code = '<span class="k">if</span> (<span class="sym-2 sym">a</span> + <span class="c">1</span>) {',
          locations = [{ from: 1, to: 5 }],
          result = helper(code, locations, 'x');
      expect(result).to.equal([
        '<span class="k">i</span>',
        '<span class="k x">f</span>',
        '<span class=" x"> (</span>',
        '<span class="sym-2 sym x">a</span>',
        '<span class=""> + </span>',
        '<span class="c">1</span>',
        '<span class="">) {</span>'
      ].join(''));
    });

    it('should mark two locations', function () {
      var code = 'abcdefghijklmnopqrst',
          locations = [
            { from: 1, to: 6 },
            { from: 11, to: 16 }
          ],
          result = helper(code, locations, 'x');
      expect(result).to.equal([
        '<span class="">a</span>',
        '<span class=" x">bcdef</span>',
        '<span class="">ghijk</span>',
        '<span class=" x">lmnop</span>',
        '<span class="">qrst</span>'
      ].join(''));
    });

    it('should mark one locations', function () {
      var code = '<span class="cppd"> * Copyright (C) 2008-2014 SonarSource</span>',
          locations = [{ from: 15, to: 20 }],
          result = helper(code, locations, 'x');
      expect(result).to.equal([
        '<span class="cppd"> * Copyright (C</span>',
        '<span class="cppd x">) 200</span>',
        '<span class="cppd">8-2014 SonarSource</span>'
      ].join(''));
    });

    it('should mark two locations', function () {
      var code = '<span class="cppd"> * Copyright (C) 2008-2014 SonarSource</span>',
          locations = [
            { from: 24, to: 29 },
            { from: 15, to: 20 }
          ],
          result = helper(code, locations, 'x');
      expect(result).to.equal([
        '<span class="cppd"> * Copyright (C</span>',
        '<span class="cppd x">) 200</span>',
        '<span class="cppd">8-20</span>',
        '<span class="cppd x">14 So</span>',
        '<span class="cppd">narSource</span>'
      ].join(''));
    });

    it('should parse line with < and >', function () {
      var code = '<span class="j">#include &lt;stdio.h&gt;</span>',
          result = helper(code, []);
      expect(result).to.equal('<span class="j">#include &lt;stdio.h&gt;</span>');
    });
  });
});

