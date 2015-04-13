/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
/* global casper:false */

var lib = require('../lib'),
    testName = lib.testName('Quality Profiles');

lib.initMessages();
lib.changeWorkingDirectory('quality-profiles');
lib.configureCasper();


casper.test.begin(testName('Should Show List'), 9, function (test) {
  casper
      .start(lib.buildUrl('profiles'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/qualityprofiles/search', 'search.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/quality-profiles/app.js']);
        });
      })

      .then(function () {
        casper.waitForSelector('.js-list .list-group-item');
      })

      .then(function () {
        test.assertElementCount('.js-list .list-group-item', 5);
        test.assertSelectorContains('.js-list .list-group-item', 'Sonar way');
        test.assertSelectorContains('.js-list .list-group-item', 'PSR-2');

        test.assertElementCount('.js-list-language', 4);
        test.assertSelectorContains('.js-list-language', 'Java');
        test.assertSelectorContains('.js-list-language', 'JavaScript');
        test.assertSelectorContains('.js-list-language', 'PHP');
        test.assertSelectorContains('.js-list-language', 'Python');

        test.assertElementCount('.js-list .note', 4);
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Should Show Details'), 9, function (test) {
  casper
      .start(lib.buildUrl('profiles'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/qualityprofiles/search', 'search.json');
        lib.mockRequestFromFile('/api/rules/search', 'rules.json');
        lib.mockRequestFromFile('/api/qualityprofiles/inheritance', 'inheritance.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/quality-profiles/app.js']);
        });
      })

      .then(function () {
        casper.waitForSelector('.js-list .list-group-item');
      })

      .then(function () {
        casper.click('.js-list .list-group-item[data-key="java-sonar-way-67887"]');
        casper.waitForSelector('.search-navigator-header-component');
      })

      .then(function () {
        test.assertElementCount('.js-list .list-group-item.active', 1);
        test.assertSelectorContains('.js-list .list-group-item.active', 'Sonar way');

        test.assertSelectorContains('.search-navigator-workspace-header', 'Sonar way');
        test.assertSelectorContains('.search-navigator-workspace-header', 'Java');
        test.assertExists('#quality-profile-backup');
        test.assertExists('#quality-profile-rename');
        test.assertExists('#quality-profile-copy');
        test.assertDoesntExist('#quality-profile-delete');
        test.assertDoesntExist('#quality-profile-set-as-default');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Should Show Inheritance Details'), 10, function (test) {
  casper
      .start(lib.buildUrl('profiles'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/qualityprofiles/search', 'search-inheritance.json');
        lib.mockRequestFromFile('/api/rules/search', 'rules.json');
        lib.mockRequestFromFile('/api/qualityprofiles/inheritance', 'inheritance-plus.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/quality-profiles/app.js']);
        });
      })

      .then(function () {
        casper.waitForSelector('.js-list .list-group-item');
      })

      .then(function () {
        casper.click('.js-list .list-group-item[data-key="java-inherited-profile-85155"]');
        casper.waitForSelector('.search-navigator-header-component');
      })

      .then(function () {
        test.assertElementCount('#quality-profile-ancestors li', 1);
        test.assertSelectorContains('#quality-profile-ancestors', 'Sonar way');
        test.assertSelectorContains('#quality-profile-ancestors', '161');

        test.assertElementCount('#quality-profile-inheritance-current', 1);
        test.assertSelectorContains('#quality-profile-inheritance-current', 'Inherited Profile');
        test.assertSelectorContains('#quality-profile-inheritance-current', '163');
        test.assertSelectorContains('#quality-profile-inheritance-current', '7');

        test.assertElementCount('#quality-profile-children li', 1);
        test.assertSelectorContains('#quality-profile-children', 'Second Level Inherited Profile');
        test.assertSelectorContains('#quality-profile-children', '165');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Should Show Selected Projects'), 2, function (test) {
  casper
      .start(lib.buildUrl('profiles'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/qualityprofiles/search', 'search.json');
        lib.mockRequestFromFile('/api/rules/search', 'rules.json');
        lib.mockRequestFromFile('/api/qualityprofiles/projects*', 'projects.json');
        lib.mockRequestFromFile('/api/qualityprofiles/inheritance', 'inheritance.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/quality-profiles/app.js']);
        });
      })

      .then(function () {
        casper.waitForSelector('.js-list .list-group-item');
      })

      .then(function () {
        casper.click('.js-list .list-group-item[data-key="php-psr-2-46772"]');
        casper.waitForSelector('#quality-profile-projects');
      })

      .then(function () {
        lib.waitForElementCount('#quality-profile-projects .select-list-list li', 2);
      })

      .then(function () {
        test.assertSelectorContains('#quality-profile-projects .select-list-list li', 'CSS');
        test.assertSelectorContains('#quality-profile-projects .select-list-list li', 'http-request-parent');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Copy Profile'), 5, function (test) {
  casper
      .start(lib.buildUrl('profiles'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/qualityprofiles/search', 'search.json');
        lib.mockRequestFromFile('/api/rules/search', 'rules.json');
        lib.mockRequestFromFile('/api/qualityprofiles/inheritance', 'inheritance.json');
        lib.mockRequestFromFile('/api/qualityprofiles/copy', 'copy.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/quality-profiles/app.js']);
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.js-list .list-group-item');
      })

      .then(function () {
        test.assertElementCount('.js-list .list-group-item', 5);
        casper.click('.js-list .list-group-item[data-key="java-sonar-way-67887"]');
        casper.waitForSelector('#quality-profile-copy');
      })

      .then(function () {
        casper.click('#quality-profile-copy');
        casper.waitForSelector('.modal');
      })

      .then(function () {
        casper.evaluate(function () {
          jQuery('#copy-name').val('Copied Profile');
        });
        casper.click('#copy-submit');
        casper.waitForSelectorTextChange('.search-navigator-header-component');
      })

      .then(function () {
        test.assertElementCount('.js-list .list-group-item', 6);
        test.assertSelectorContains('.js-list .list-group-item.active', 'Copied Profile');
        test.assertSelectorContains('.search-navigator-header-component', 'Copied Profile');
        test.assertSelectorContains('.search-navigator-header-component', 'Java');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Rename Profile'), 2, function (test) {
  casper
      .start(lib.buildUrl('profiles'), function () {
        lib.setDefaultViewport();

        this.searchMock = lib.mockRequestFromFile('/api/qualityprofiles/search', 'search.json');
        lib.mockRequestFromFile('/api/rules/search', 'rules.json');
        lib.mockRequestFromFile('/api/qualityprofiles/inheritance', 'inheritance.json');
        lib.mockRequest('/api/qualityprofiles/rename', '{}');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/quality-profiles/app.js']);
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.js-list .list-group-item');
      })

      .then(function () {
        casper.click('.js-list .list-group-item[data-key="java-sonar-way-67887"]');
        casper.waitForSelector('#quality-profile-rename');
      })

      .then(function () {
        casper.click('#quality-profile-rename');
        casper.waitForSelector('.modal');
      })

      .then(function () {
        lib.clearRequestMock(this.searchMock);
        lib.mockRequestFromFile('/api/qualityprofiles/search', 'search-renamed.json');

        casper.evaluate(function () {
          jQuery('#new-name').val('Renamed Profile');
        });
        casper.click('#rename-submit');
        casper.waitForSelectorTextChange('.search-navigator-header-component');
      })

      .then(function () {
        test.assertSelectorContains('.js-list .list-group-item.active', 'Renamed Profile');
        test.assertSelectorContains('.search-navigator-header-component', 'Renamed Profile');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Make Profile Default'), 4, function (test) {
  casper
      .start(lib.buildUrl('profiles'), function () {
        lib.setDefaultViewport();

        this.searchMock = lib.mockRequestFromFile('/api/qualityprofiles/search', 'search.json');
        lib.mockRequestFromFile('/api/rules/search', 'rules.json');
        lib.mockRequestFromFile('/api/qualityprofiles/inheritance', 'inheritance.json');
        lib.mockRequest('/api/qualityprofiles/set_default', '{}');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/quality-profiles/app.js']);
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.js-list .list-group-item');
      })

      .then(function () {
        test.assertDoesntExist('.js-list .list-group-item[data-key="php-psr-2-46772"] .note');
        test.assertExists('.js-list .list-group-item[data-key="php-sonar-way-10778"] .note');
        casper.click('.js-list .list-group-item[data-key="php-psr-2-46772"]');
        casper.waitForSelector('#quality-profile-set-as-default');
      })

      .then(function () {
        lib.clearRequestMock(this.searchMock);
        lib.mockRequestFromFile('/api/qualityprofiles/search', 'search-another-default.json');

        casper.click('#quality-profile-set-as-default');
        casper.waitWhileSelector('.js-list .list-group-item[data-key="php-sonar-way-10778"] .note');
      })

      .then(function () {
        test.assertDoesntExist('#quality-profile-set-as-default');
        test.assertExists('.js-list .list-group-item[data-key="php-psr-2-46772"] .note');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Delete Profile'), 2, function (test) {
  casper
      .start(lib.buildUrl('profiles'), function () {
        lib.setDefaultViewport();

        this.searchMock = lib.mockRequestFromFile('/api/qualityprofiles/search', 'search-with-copy.json');
        lib.mockRequestFromFile('/api/rules/search', 'rules.json');
        lib.mockRequestFromFile('/api/qualityprofiles/inheritance', 'inheritance.json');
        lib.mockRequest('/api/qualityprofiles/delete', '{}');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/quality-profiles/app.js']);
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.js-list .list-group-item');
      })

      .then(function () {
        test.assertElementCount('.js-list .list-group-item', 6);
        casper.click('.js-list .list-group-item[data-key="java-copied-profile-11711"]');
        casper.waitForSelector('#quality-profile-delete');
      })

      .then(function () {
        casper.click('#quality-profile-delete');
        casper.waitForSelector('.modal');
      })

      .then(function () {
        lib.clearRequestMock(this.searchMock);
        lib.mockRequestFromFile('/api/qualityprofiles/search', 'search.json');

        casper.click('#delete-submit');
        lib.waitForElementCount('.js-list .list-group-item', 5);
      })

      .then(function () {
        test.assertSelectorDoesntContain('.js-list .list-group-item', 'Copied Profile');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Create Profile'), 2, function (test) {
  casper
      .start(lib.buildUrl('profiles'), function () {
        lib.setDefaultViewport();

        this.searchMock = lib.mockRequestFromFile('/api/qualityprofiles/search', 'search.json');
        lib.mockRequestFromFile('/api/rules/search', 'rules.json');
        lib.mockRequestFromFile('/api/qualityprofiles/inheritance', 'inheritance.json');
        lib.mockRequestFromFile('/api/qualityprofiles/create', 'create.json');
        lib.mockRequestFromFile('/api/languages/list', 'languages.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/quality-profiles/app.js']);
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.js-list .list-group-item');
      })

      .then(function () {
        test.assertElementCount('.js-list .list-group-item', 5);
        casper.click('#quality-profiles-create');
        casper.waitForSelector('.modal');
      })

      .then(function () {
        lib.clearRequestMock(this.searchMock);
        lib.mockRequestFromFile('/api/qualityprofiles/search', 'search-with-copy.json');

        casper.evaluate(function () {
          jQuery('#create-profile-name').val('Copied Profile');
          jQuery('#create-profile-language').val('java');
        });
        casper.click('#create-profile-submit');
        lib.waitForElementCount('.js-list .list-group-item', 6);
      })

      .then(function () {
        test.assertExists('.js-list .list-group-item.active[data-key="java-copied-profile-11711"]');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Restore Built-in Profiles'), 2, function (test) {
  casper
      .start(lib.buildUrl('profiles'), function () {
        lib.setDefaultViewport();

        this.searchMock = lib.mockRequestFromFile('/api/qualityprofiles/search', 'search-modified.json');
        lib.mockRequestFromFile('/api/rules/search', 'rules.json');
        lib.mockRequestFromFile('/api/qualityprofiles/inheritance', 'inheritance.json');
        lib.mockRequest('/api/qualityprofiles/restore_built_in', '{}');
        lib.mockRequestFromFile('/api/languages/list', 'languages.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/quality-profiles/app.js']);
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.js-list .list-group-item');
      })

      .then(function () {
        test.assertElementCount('.js-list .list-group-item', 1);
        casper.click('#quality-profiles-restore-built-in');
        casper.waitForSelector('.modal');
      })

      .then(function () {
        lib.clearRequestMock(this.searchMock);
        lib.mockRequestFromFile('/api/qualityprofiles/search', 'search.json');

        casper.evaluate(function () {
          jQuery('#restore-built-in-profiles-language').val('java');
        });
        casper.click('#restore-built-in-profiles-submit');
        lib.waitForElementCount('.js-list .list-group-item', 5);
      })

      .then(function () {
        test.assertSelectorContains('.js-list .list-group-item', 'Sonar way');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Change Parent'), 1, function (test) {
  casper
      .start(lib.buildUrl('profiles'), function () {
        lib.setDefaultViewport();

        this.searchMock = lib.mockRequestFromFile('/api/qualityprofiles/search', 'search-change-parent.json');
        lib.mockRequestFromFile('/api/rules/search', 'rules.json');
        this.inheritanceMock = lib.mockRequestFromFile('/api/qualityprofiles/inheritance', 'inheritance-change-parent.json');
        lib.mockRequest('/api/qualityprofiles/change_parent', '{}');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/quality-profiles/app.js']);
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.js-list .list-group-item');
      })

      .then(function () {
        casper.click('.js-list .list-group-item[data-key="java-inherited-profile-85155"]');
        casper.waitForSelector('#quality-profile-change-parent');
      })

      .then(function () {
        casper.click('#quality-profile-change-parent');
        casper.waitForSelector('.modal');
      })

      .then(function () {
        lib.clearRequestMock(this.searchMock);
        lib.mockRequestFromFile('/api/qualityprofiles/search', 'search-changed-parent.json');
        lib.clearRequestMock(this.inheritanceMock);
        lib.mockRequestFromFile('/api/qualityprofiles/inheritance', 'inheritance-changed-parent.json');

        casper.evaluate(function () {
          jQuery('#change-profile-parent').val('java-another-profile-00609');
        });
        casper.click('#change-profile-parent-submit');
        casper.waitForSelectorTextChange('#quality-profile-ancestors');
      })

      .then(function () {
        test.assertSelectorContains('#quality-profile-ancestors', 'Another Profile');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
