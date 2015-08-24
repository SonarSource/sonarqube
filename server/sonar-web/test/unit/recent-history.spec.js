define(function (require) {
  var bdd = require('intern!bdd');
  var assert = require('intern/chai!assert');

  var RecentHistory = require('../../build/js/libs/recent-history');

  bdd.describe('RecentHistory', function () {
    bdd.beforeEach(function () {
      RecentHistory.clear();
    });

    bdd.it('should clear history', function () {
      assert.equal(RecentHistory.getRecentHistory().length, 0);
      RecentHistory.add('project-key', 'Project Name', 'trk');
      assert.equal(RecentHistory.getRecentHistory().length, 1);
      RecentHistory.clear();
      assert.equal(RecentHistory.getRecentHistory().length, 0);
    });

    bdd.it('should add a new item', function () {
      RecentHistory.add('project-key', 'Project Name', 'trk');
      assert.deepEqual(RecentHistory.getRecentHistory(), [{ key: 'project-key', name: 'Project Name', icon: 'trk' }]);
    });

    bdd.it('should replace existing item', function () {
      RecentHistory.add('project-key', 'Project Name', 'trk');
      assert.deepEqual(RecentHistory.getRecentHistory(), [{ key: 'project-key', name: 'Project Name', icon: 'trk' }]);
      RecentHistory.add('project-key', 'Another', 'brc');
      assert.deepEqual(RecentHistory.getRecentHistory(), [{ key: 'project-key', name: 'Another', icon: 'brc' }]);
    });

    bdd.it('should limit the number of items', function () {
      assert.equal(RecentHistory.getRecentHistory().length, 0);
      for (var i = 0; i < 10; i++) {
        RecentHistory.add('key-' + i, 'Project ' + i, 'trk');
      }
      assert.equal(RecentHistory.getRecentHistory().length, 10);
      RecentHistory.add('project-key', 'Project Name', 'trk');
      assert.equal(RecentHistory.getRecentHistory().length, 10);
      assert.deepEqual(RecentHistory.getRecentHistory()[0], { key: 'project-key', name: 'Project Name', icon: 'trk' });
    });
  });
});
