#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#

define
  # Coverage

  # UT
  'coverage':                         { tab: 'coverage', item: '.js-filter-lines-to-cover' }
  'lines_to_cover':                   { tab: 'coverage', item: '.js-filter-lines-to-cover' }
  'uncovered_lines':                  { tab: 'coverage', item: '.js-filter-uncovered-lines' }
  'line_coverage':                    { tab: 'coverage', item: '.js-filter-lines-to-cover' }
  'conditions_to_cover':              { tab: 'coverage', item: '.js-filter-branches-to-cover' }
  'uncovered_conditions':             { tab: 'coverage', item: '.js-filter-uncovered-branches' }
  'branch_coverage':                  { tab: 'coverage', item: '.js-filter-branches-to-cover' }
  'new_coverage':                     { tab: 'coverage', item: '.js-filter-lines-to-cover' }
  'new_uncovered_lines':              { tab: 'coverage', item: '.js-filter-uncovered-lines' }
  'new_line_coverage':                { tab: 'coverage', item: '.js-filter-lines-to-cover' }
  'new_lines_to_cover':               { tab: 'coverage', item: '.js-filter-lines-to-cover' }
  'new_branch_coverage':              { tab: 'coverage', item: '.js-filter-branches-to-cover' }
  'new_conditions_to_cover':          { tab: 'coverage', item: '.js-filter-branches-to-cover' }
  'new_uncovered_conditions':         { tab: 'coverage', item: '.js-filter-uncovered-branches' }

  # IT
  'it_coverage':                      { tab: 'coverage', item: '.js-filter-lines-to-cover-it' }
  'it_lines_to_cover':                { tab: 'coverage', item: '.js-filter-lines-to-cover-it' }
  'it_uncovered_lines':               { tab: 'coverage', item: '.js-filter-uncovered-lines-it' }
  'it_line_coverage':                 { tab: 'coverage', item: '.js-filter-lines-to-cover-it' }
  'it_conditions_to_cover':           { tab: 'coverage', item: '.js-filter-branches-to-cover-it' }
  'it_uncovered_conditions':          { tab: 'coverage', item: '.js-filter-uncovered-branches-it' }
  'it_branch_coverage':               { tab: 'coverage', item: '.js-filter-branches-to-cover-it' }
  'new_it_coverage':                  { tab: 'coverage', item: '.js-filter-lines-to-cover-it' }
  'new_it_uncovered_lines':           { tab: 'coverage', item: '.js-filter-uncovered-lines-it' }
  'new_it_line_coverage':             { tab: 'coverage', item: '.js-filter-lines-to-cover-it' }
  'new_it_lines_to_cover':            { tab: 'coverage', item: '.js-filter-lines-to-cover-it' }
  'new_it_branch_coverage':           { tab: 'coverage', item: '.js-filter-branches-to-cover-it' }
  'new_it_conditions_to_cover':       { tab: 'coverage', item: '.js-filter-branches-to-cover-it' }
  'new_it_uncovered_conditions':      { tab: 'coverage', item: '.js-filter-uncovered-branches-it' }
  
  # Overall
  'overall_coverage':                 { tab: 'coverage', item: '.js-filter-lines-to-cover-overall' }
  'overall_lines_to_cover':           { tab: 'coverage', item: '.js-filter-lines-to-cover-overall' }
  'overall_uncovered_lines':          { tab: 'coverage', item: '.js-filter-uncovered-lines-overall' }
  'overall_line_coverage':            { tab: 'coverage', item: '.js-filter-lines-to-cover-overall' }
  'overall_conditions_to_cover':      { tab: 'coverage', item: '.js-filter-branches-to-cover-overall' }
  'overall_uncovered_conditions':     { tab: 'coverage', item: '.js-filter-uncovered-branches-overall' }
  'overall_branch_coverage':          { tab: 'coverage', item: '.js-filter-branches-to-cover-overall' }
  'new_overall_coverage':             { tab: 'coverage', item: '.js-filter-lines-to-cover-overall' }
  'new_overall_uncovered_lines':      { tab: 'coverage', item: '.js-filter-uncovered-lines-overall' }
  'new_overall_line_coverage':        { tab: 'coverage', item: '.js-filter-lines-to-cover-overall' }
  'new_overall_lines_to_cover':       { tab: 'coverage', item: '.js-filter-lines-to-cover-overall' }
  'new_overall_branch_coverage':      { tab: 'coverage', item: '.js-filter-branches-to-cover-overall' }
  'new_overall_conditions_to_cover':  { tab: 'coverage', item: '.js-filter-branches-to-cover-overall' }
  'new_overall_uncovered_conditions': { tab: 'coverage', item: '.js-filter-uncovered-branches-overall' }


  # Issues
  'violations':                       { tab: 'issues', item: '.js-filter-unresolved-issues' }
  'blocker_violations':               { tab: 'issues', item: '.js-filter-BLOCKER-issues' }
  'critical_violations':              { tab: 'issues', item: '.js-filter-CRITICAL-issues' }
  'major_violations':                 { tab: 'issues', item: '.js-filter-MAJOR-issues' }
  'minor_violations':                 { tab: 'issues', item: '.js-filter-MINOR-issues' }
  'info_violations':                  { tab: 'issues', item: '.js-filter-INFO-issues' }
  'new_violations':                   { tab: 'issues', item: '.js-filter-unresolved-issues' }
  'new_blocker_violations':           { tab: 'issues', item: '.js-filter-BLOCKER-issues' }
  'new_critical_violations':          { tab: 'issues', item: '.js-filter-CRITICAL-issues' }
  'new_major_violations':             { tab: 'issues', item: '.js-filter-MAJOR-issues' }
  'new_minor_violations':             { tab: 'issues', item: '.js-filter-MINOR-issues' }
  'new_info_violations':              { tab: 'issues', item: '.js-filter-INFO-issues' }
  'false_positive_issues':            { tab: 'issues', item: '.js-filter-false-positive-issues' }
  'sqale_index':                      { tab: 'issues', item: '.js-filter-unresolved-issues' }
  'sqale_debt_ratio':                 { tab: 'issues', item: '.js-filter-unresolved-issues' }
  'sqale_rating':                     { tab: 'issues', item: '.js-filter-unresolved-issues' }
  'new_technical_debt':               { tab: 'issues', item: '.js-filter-unresolved-issues' }
  'open_issues':                      { tab: 'issues', item: '.js-filter-open-issues' }
  'reopened_issues':                  { tab: 'issues', item: '.js-filter-open-issues' }
  'confirmed_issues':                 { tab: 'issues', item: '.js-filter-unresolved-issues' }


  # Duplications
  'duplicated_lines':                 { tab: 'duplications', item: '.js-filter-duplications' }
  'duplicated_blocks':                { tab: 'duplications', item: '.js-filter-duplications' }
  'duplicated_files':                 { tab: 'duplications', item: '.js-filter-duplications' }
  'duplicated_lines_density':         { tab: 'duplications', item: '.js-filter-duplications' }


  # Tests
  'tests':                            { tab: 'tests', item: null }
  'test_success_density':             { tab: 'tests', item: null }
  'skipped_tests':                    { tab: 'tests', item: null }
  'test_failures':                    { tab: 'tests', item: null }
  'test_errors':                      { tab: 'tests', item: null }
  'test_execution_time':              { tab: 'tests', item: null }
