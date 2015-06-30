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

      bdd.it('should do a transition', function () {
        new Issue({ key: 'issue-key' }).transition('RESOLVED');
        assert.isTrue(stub.calledOnce);
        assert.equal(stub.firstCall.args[0].url, '/api/issues/do_transition');
        assert.deepEqual(stub.firstCall.args[0].data, { issue: 'issue-key', transition: 'RESOLVED' });
      });
    });
  });
});
