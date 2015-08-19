define(function (require) {
  var bdd = require('intern!bdd');
  var assert = require('intern/chai!assert');

  var helper = require('build/js/components/source-viewer/helpers/code-with-issue-locations-helper');

  bdd.describe('Code With Issue Locations Helper', function () {
    bdd.it('should exist', function () {
      assert.equal(typeof helper, 'function');
    });

    bdd.it('should mark one location', function () {
      var code = '<span class="k">if</span> (<span class="sym-2 sym">a</span> + <span class="c">1</span>) {',
          locations = [{ from: 1, to: 5 }],
          result = helper(code, locations, 'x');
      assert.equal(result,
          '<span class="k">i</span><span class="k x">f</span><span class=" x"> (</span><span class="sym-2 sym x">a</span><span class=""> + </span><span class="c">1</span><span class="">) {</span>');
    });

    bdd.it('should mark two locations', function () {
      var code = 'abcdefghijklmnopqrst',
          locations = [
            { from: 1, to: 6 },
            { from: 11, to: 16 }
          ],
          result = helper(code, locations, 'x');
      assert.equal(result,
          ['<span class="">a</span>',
           '<span class=" x">bcdef</span>',
           '<span class="">ghijk</span>',
           '<span class=" x">lmnop</span>',
           '<span class="">qrst</span>'].join(''));
    });

    bdd.it('should mark one locations', function () {
      var code = '<span class="cppd"> * Copyright (C) 2008-2014 SonarSource</span>',
          locations = [{ from: 15, to: 20 }],
          result = helper(code, locations, 'x');
      assert.equal(result,
          '<span class="cppd"> * Copyright (C</span><span class="cppd x">) 200</span><span class="cppd">8-2014 SonarSource</span>');
    });

    bdd.it('should mark two locations', function () {
      var code = '<span class="cppd"> * Copyright (C) 2008-2014 SonarSource</span>',
          locations = [
            { from: 24, to: 29 },
            { from: 15, to: 20 }
          ],
          result = helper(code, locations, 'x');
      assert.equal(result,
          '<span class="cppd"> * Copyright (C</span><span class="cppd x">) 200</span><span class="cppd">8-20</span><span class="cppd x">14 So</span><span class="cppd">narSource</span>');
      //   <span class="cppd"> * Copyright (C</span><span class="cppd x">) 200</span><span class="cppd">8-20</span><span class="cppd x">4 So</span><span class="cppd">narSource</span>
    });

    bdd.it('should parse line with < and >', function () {
      var code = '<span class="j">#include &lt;stdio.h&gt;</span>',
          result = helper(code, []);
      assert.equal(result, '<span class="j">#include &lt;stdio.h&gt;</span>');
    });
  });
});

