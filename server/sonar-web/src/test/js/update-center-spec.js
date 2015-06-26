/* global describe:false, it:false */
var lib = require('../lib');

describe('Update Center App', function () {

  it('should show plugin card', 16, function (casper, test) {
    return casper
        .start(lib.buildUrl('base#installed'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/plugins/installed', 'installed.json');
          lib.fmock('/api/plugins/updates', 'updates.json');
          lib.fmock('/api/plugins/pending', 'pending.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/update-center/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.js-plugin-name');
        })

        .then(function () {
          test.assertElementCount('li[data-id]', 5);
          test.assertSelectorContains('li[data-id="scmgit"] .js-plugin-name', 'Git');
          test.assertSelectorContains('li[data-id="scmgit"] .js-plugin-category', 'Integration');
          test.assertSelectorContains('li[data-id="scmgit"] .js-plugin-description', 'Git SCM Provider.');
          test.assertSelectorContains('li[data-id="scmgit"] .js-plugin-installed-version', '1.0');
          test.assertElementCount('li[data-id="scmgit"] .js-update-version', 1);
          test.assertSelectorContains('li[data-id="scmgit"] .js-update-version', '1.1');
          test.assertElementCount('li[data-id="scmgit"] .js-changelog', 1);
          test.assertElementCount('li[data-id="scmgit"] .js-plugin-homepage', 1);
          test.assertElementCount('li[data-id="scmgit"] .js-plugin-issues', 1);
          test.assertDoesntExist('li[data-id="scmgit"] .js-plugin-terms');
          test.assertSelectorContains('li[data-id="scmgit"] .js-plugin-license', 'GNU LGPL 3');
          test.assertSelectorContains('li[data-id="scmgit"] .js-plugin-organization', 'SonarSource');
          test.assertElementCount('li[data-id="scmgit"] .js-update', 1);
          test.assertElementCount('li[data-id="scmgit"] .js-uninstall', 1);
          test.assertDoesntExist('li[data-id="scmgit"] .js-install');
        });
  });

  it('should show system update', 8, function (casper, test) {
    return casper
        .start(lib.buildUrl('base#updates'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/plugins/installed', 'installed.json');
          lib.fmock('/api/plugins/updates', 'updates.json');
          lib.fmock('/api/plugins/pending', 'pending.json');
          lib.fmock('/api/system/upgrades', 'system-updates.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/update-center/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.js-plugin-name');
        })

        .then(function () {
          test.assertElementCount('li[data-system]', 1);
          test.assertSelectorContains('li[data-system] .js-plugin-name', 'SonarQube 5.3');
          test.assertSelectorContains('li[data-system] .js-plugin-category', 'System Update');
          test.assertSelectorContains('li[data-system] .js-plugin-description', 'New!');
          test.assertElementCount('li[data-system] .js-plugin-release-notes', 1);
          test.assertElementCount('li[data-system] .js-plugin-date', 1);
          test.assertElementCount('li[data-system] .js-plugin-update-steps', 1);
          test.assertElementCount('li[data-system] .js-plugin-update-steps > li', 4);
        });
  });

  it('should show installed', 3, function (casper, test) {
    return casper
        .start(lib.buildUrl('base#installed'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/plugins/installed', 'installed.json');
          lib.fmock('/api/plugins/updates', 'updates.json');
          lib.fmock('/api/plugins/pending', 'pending.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/update-center/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.js-plugin-name');
        })

        .then(function () {
          test.assertElementCount('li[data-id]', 5);
          test.assertExists('li[data-id="scmgit"]');
          test.assertExists('li[data-id="javascript"]');
        });
  });

  it('should show updates', 3, function (casper, test) {
    return casper
        .start(lib.buildUrl('base#updates'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/plugins/installed', 'installed.json');
          lib.fmock('/api/plugins/updates', 'updates.json');
          lib.fmock('/api/plugins/pending', 'pending.json');
          lib.fmock('/api/system/upgrades', 'system-updates.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/update-center/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.js-plugin-name');
        })

        .then(function () {
          test.assertElementCount('li[data-id]', 4);
          test.assertExists('li[data-id="scmgit"]');
          test.assertDoesntExist('li[data-id="javascript"]');
        });
  });

  it('should show available', 3, function (casper, test) {
    return casper
        .start(lib.buildUrl('base#available'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/plugins/available', 'available.json');
          lib.fmock('/api/plugins/pending', 'pending.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/update-center/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.js-plugin-name');
        })

        .then(function () {
          test.assertElementCount('li[data-id]', 3);
          test.assertDoesntExist('li[data-id="scmgit"]');
          test.assertExists('li[data-id="abap"]');
        });
  });

  it('should switch between views', 18, function (casper, test) {
    return casper
        .start(lib.buildUrl('base#installed'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/plugins/installed', 'installed.json');
          lib.fmock('/api/plugins/updates', 'updates.json');
          lib.fmock('/api/plugins/pending', 'pending.json');
          lib.fmock('/api/plugins/available', 'available.json');
          lib.fmock('/api/system/upgrades', 'system-updates.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/update-center/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.js-plugin-name');
        })

        .then(function () {
          test.assertElementCount('li[data-id]', 5);
          test.assertExists('li[data-id="javascript"]');
          test.assertExists('#update-center-filter-installed:checked');
          casper.click('#update-center-filter-available');
          casper.waitForSelector('li[data-id="abap"]');
        })

        .then(function () {
          test.assertElementCount('li[data-id]', 3);
          test.assertExists('li[data-id="abap"]');
          test.assertExists('#update-center-filter-available:checked');
          casper.click('#update-center-filter-updates');
          casper.waitForSelector('li[data-id="scmgit"]');
        })

        .then(function () {
          test.assertElementCount('li[data-id]', 4);
          test.assertExists('li[data-id="scmgit"]');
          test.assertExists('#update-center-filter-updates:checked');
          casper.click('#update-center-filter-installed');
          casper.waitForSelector('li[data-id="javascript"]');
        })

        .then(function () {
          test.assertElementCount('li[data-id]', 5);
          test.assertExists('li[data-id="javascript"]');
          test.assertExists('#update-center-filter-installed:checked');
          casper.click('#update-center-filter-available');
          casper.waitForSelector('li[data-id="abap"]');
        })

        .then(function () {
          test.assertElementCount('li[data-id]', 3);
          test.assertExists('li[data-id="abap"]');
          test.assertExists('#update-center-filter-available:checked');
          casper.click('#update-center-filter-updates');
          casper.waitForSelector('li[data-id="scmgit"]');
        })

        .then(function () {
          test.assertElementCount('li[data-id]', 4);
          test.assertExists('li[data-id="scmgit"]');
          test.assertExists('#update-center-filter-updates:checked');
        });
  });

  it('should search', 5, function (casper, test) {
    return casper
        .start(lib.buildUrl('base#installed'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/plugins/installed', 'installed.json');
          lib.fmock('/api/plugins/updates', 'updates.json');
          lib.fmock('/api/plugins/pending', 'pending.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/update-center/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.js-plugin-name');
        })

        .then(function () {
          test.assertElementCount('li[data-id]', 5);
          test.assertDoesntExist('li.hidden[data-id]');
          casper.evaluate(function () {
            jQuery('#update-center-search-query').val('jA');
          });
          casper.click('#update-center-search-submit');
          casper.waitForSelector('li.hidden[data-id]');
        })

        .then(function () {
          test.assertElementCount('li[data-id]', 5);
          test.assertElementCount('li.hidden[data-id]', 3);
          test.assertSelectorContains('li:not(.hidden)[data-id] .js-plugin-name', 'JavaScript');
        });
  });

  it('should show plugin changelog', 4, function (casper, test) {
    return casper
        .start(lib.buildUrl('base#installed'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/plugins/installed', 'installed.json');
          lib.fmock('/api/plugins/updates', 'updates.json');
          lib.fmock('/api/plugins/pending', 'pending.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/update-center/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.js-plugin-name');
        })

        .then(function () {
          casper.click('li[data-id="python"] .js-changelog');
          casper.waitForSelector('.bubble-popup');
        })

        .then(function () {
          test.assertElementCount('.bubble-popup .js-plugin-changelog-version', 2);
          test.assertElementCount('.bubble-popup .js-plugin-changelog-date', 2);
          test.assertElementCount('.bubble-popup .js-plugin-changelog-link', 2);
          test.assertElementCount('.bubble-popup .js-plugin-changelog-description', 2);
        });
  });

  it('should update plugin', 1, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/plugins/installed', 'installed.json');
          lib.fmock('/api/plugins/updates', 'updates.json');
          lib.fmock('/api/plugins/pending', 'pending.json');
          lib.smock('/api/plugins/update', '{}', { data: { key: 'scmgit' } });
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/update-center/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.js-plugin-name');
        })

        .then(function () {
          casper.click('li[data-id="scmgit"] .js-update');
          casper.waitWhileSelector('li[data-id="scmgit"] .js-spinner');
        })

        .then(function () {
          test.assertSelectorContains('li[data-id="scmgit"]', 'To Be Installed');
        });
  });

  it('should uninstall plugin', 1, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/plugins/installed', 'installed.json');
          lib.fmock('/api/plugins/updates', 'updates.json');
          lib.fmock('/api/plugins/pending', 'pending.json');
          lib.smock('/api/plugins/uninstall', '{}', { data: { key: 'scmgit' } });
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/update-center/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.js-plugin-name');
        })

        .then(function () {
          casper.click('li[data-id="scmgit"] .js-uninstall');
          casper.waitWhileSelector('li[data-id="scmgit"] .js-spinner');
        })

        .then(function () {
          test.assertSelectorContains('li[data-id="scmgit"]', 'To Be Uninstalled');
        });
  });

  it('should install plugin', 1, function (casper, test) {
    return casper
        .start(lib.buildUrl('base#available'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/plugins/available', 'available.json');
          lib.fmock('/api/plugins/pending', 'pending.json');
          lib.smock('/api/plugins/install', '{}', { data: { key: 'abap' } });
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/update-center/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.js-plugin-name');
        })

        .then(function () {
          casper.click('li[data-id="abap"] .js-install');
          casper.waitWhileSelector('li[data-id="abap"] .js-spinner');
        })

        .then(function () {
          test.assertSelectorContains('li[data-id="abap"]', 'To Be Installed');
        });
  });

  it('should cancel all pending', 1, function (casper, test) {
    return casper
        .start(lib.buildUrl('base#available'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/plugins/available', 'available.json');
          lib.fmock('/api/plugins/pending', 'pending.json');
          lib.smock('/api/plugins/cancel_all', '{}');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/update-center/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.js-plugin-name');
        })

        .then(function () {
          test.assertExists('.js-pending');
          casper.click('.js-cancel-all');
          casper.waitUntilVisible('.js-pending');
        });
  });

});
