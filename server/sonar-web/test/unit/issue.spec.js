define(function (require) {
  var bdd = require('intern!bdd');
  var assert = require('intern/chai!assert');

  require('intern/order!build/js/libs/translate.js');
  require('intern/order!build/js/libs/third-party/jquery.js');
  require('intern/order!build/js/libs/third-party/underscore.js');
  require('intern/order!build/js/libs/third-party/backbone.js');
  require('intern/order!build/js/libs/third-party/keymaster.js');
  require('intern/order!build/js/libs/third-party/numeral.js');
  require('intern/order!build/js/libs/third-party/numeral-languages.js');
  require('intern/order!build/js/libs/application.js');
  require('intern/order!node_modules/sinon/pkg/sinon');

  var Issue = require('build/js/components/issue/models/issue');

  bdd.describe('Issue', function () {
    bdd.before(function () {
      window.baseUrl = '';
    });

    bdd.it('should have correct urlRoot', function () {
      var issue = new Issue();
      assert.equal(issue.urlRoot(), '/api/issues');
    });

    bdd.it('should parse response without root issue object', function () {
      var issue = new Issue();
      var example = { a: 1 };
      assert.deepEqual(issue.parse(example), example);
    });

    bdd.it('should parse response with the root issue object', function () {
      var issue = new Issue();
      var example = { a: 1 };
      assert.deepEqual(issue.parse({ issue: example }), example);
    });

    bdd.it('should reset attributes (no attributes initially)', function () {
      var issue = new Issue();
      var example = { a: 1 };
      issue.reset(example);
      assert.deepEqual(issue.toJSON(), example);
    });

    bdd.it('should reset attributes (override attribute)', function () {
      var issue = new Issue({ a: 2 });
      var example = { a: 1 };
      issue.reset(example);
      assert.deepEqual(issue.toJSON(), example);
    });

    bdd.it('should reset attributes (different attributes)', function () {
      var issue = new Issue({ a: 2 });
      var example = { b: 1 };
      issue.reset(example);
      assert.deepEqual(issue.toJSON(), example);
    });

    bdd.it('should unset `textRange` of a closed issue', function () {
      var issue = new Issue();
      var result = issue.parse({ issue: { status: 'CLOSED', textRange: { startLine: 5 } } });
      assert.notOk(result.textRange);
    });

    bdd.it('should unset `flows` of a closed issue', function () {
      var issue = new Issue();
      var result = issue.parse({ issue: { status: 'CLOSED', flows: [1, 2, 3] } });
      assert.deepEqual(result.flows, []);
    });

    bdd.describe('Actions', function () {
      var stub;

      bdd.beforeEach(function () {
        stub = sinon.stub(jQuery, 'ajax');
      });

      bdd.afterEach(function () {
        jQuery.ajax.restore();
      });

      bdd.it('should assign', function () {
        new Issue({ key: 'issue-key' }).assign('admin');
        assert.isTrue(stub.calledOnce);
        assert.equal(stub.firstCall.args[0].url, '/api/issues/assign');
        assert.deepEqual(stub.firstCall.args[0].data, { issue: 'issue-key', assignee: 'admin' });
      });

      bdd.it('should unassign', function () {
        new Issue({ key: 'issue-key' }).assign();
        assert.isTrue(stub.calledOnce);
        assert.equal(stub.firstCall.args[0].url, '/api/issues/assign');
        assert.deepEqual(stub.firstCall.args[0].data, { issue: 'issue-key', assignee: undefined });
      });

      bdd.it('should plan', function () {
        new Issue({ key: 'issue-key' }).plan('plan');
        assert.isTrue(stub.calledOnce);
        assert.equal(stub.firstCall.args[0].url, '/api/issues/plan');
        assert.deepEqual(stub.firstCall.args[0].data, { issue: 'issue-key', plan: 'plan' });
      });

      bdd.it('should unplan', function () {
        new Issue({ key: 'issue-key' }).plan();
        assert.isTrue(stub.calledOnce);
        assert.equal(stub.firstCall.args[0].url, '/api/issues/plan');
        assert.deepEqual(stub.firstCall.args[0].data, { issue: 'issue-key', plan: undefined });
      });

      bdd.it('should set severity', function () {
        new Issue({ key: 'issue-key' }).setSeverity('BLOCKER');
        assert.isTrue(stub.calledOnce);
        assert.equal(stub.firstCall.args[0].url, '/api/issues/set_severity');
        assert.deepEqual(stub.firstCall.args[0].data, { issue: 'issue-key', severity: 'BLOCKER' });
      });
    });

    bdd.describe('#getLinearLocations', function () {
      bdd.it('should return single line location', function () {
        var issue = new Issue({ textRange: { startLine: 1, endLine: 1, startOffset: 0, endOffset: 10 } }),
            locations = issue.getLinearLocations();
        assert.equal(locations.length, 1);

        assert.equal(locations[0].line, 1);
        assert.equal(locations[0].from, 0);
        assert.equal(locations[0].to, 10);
      });

      bdd.it('should return location not from 0', function () {
        var issue = new Issue({ textRange: { startLine: 1, endLine: 1, startOffset: 5, endOffset: 10 } }),
            locations = issue.getLinearLocations();
        assert.equal(locations.length, 1);

        assert.equal(locations[0].line, 1);
        assert.equal(locations[0].from, 5);
        assert.equal(locations[0].to, 10);
      });

      bdd.it('should return 2-lines location', function () {
        var issue = new Issue({ textRange: { startLine: 2, endLine: 3, startOffset: 5, endOffset: 10 } }),
            locations = issue.getLinearLocations();
        assert.equal(locations.length, 2);

        assert.equal(locations[0].line, 2);
        assert.equal(locations[0].from, 5);
        assert.equal(locations[0].to, 999999);

        assert.equal(locations[1].line, 3);
        assert.equal(locations[1].from, 0);
        assert.equal(locations[1].to, 10);
      });

      bdd.it('should return 3-lines location', function () {
        var issue = new Issue({ textRange: { startLine: 4, endLine: 6, startOffset: 5, endOffset: 10 } }),
            locations = issue.getLinearLocations();
        assert.equal(locations.length, 3);

        assert.equal(locations[0].line, 4);
        assert.equal(locations[0].from, 5);
        assert.equal(locations[0].to, 999999);

        assert.equal(locations[1].line, 5);
        assert.equal(locations[1].from, 0);
        assert.equal(locations[1].to, 999999);

        assert.equal(locations[2].line, 6);
        assert.equal(locations[2].from, 0);
        assert.equal(locations[2].to, 10);
      });

      bdd.it('should return [] when no location', function () {
        var issue = new Issue(),
            locations = issue.getLinearLocations();
        assert.equal(locations.length, 0);
      });
    });
  });
});
