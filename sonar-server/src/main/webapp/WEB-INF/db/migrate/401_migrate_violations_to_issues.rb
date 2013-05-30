#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2013 SonarSource
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

#
# Sonar 3.6
# See SONAR-4305
#
class MigrateViolationsToIssues < ActiveRecord::Migration

  class RuleFailure < ActiveRecord::Base
  end

  class Issue < ActiveRecord::Base
  end

  class IssueChange < ActiveRecord::Base
  end

  class User < ActiveRecord::Base
  end

  class ActionPlan < ActiveRecord::Base
  end

  PRIORITY_TO_SEVERITY = {1 => 'INFO', 2 => 'MINOR', 3 => 'MAJOR', 4 => 'CRITICAL', 5 => 'BLOCKER'}

  def self.up
    truncate_issues

    violation_ids = ActiveRecord::Base.connection.select_rows('select id from rule_failures')

    one_year_ago = Time.now.years_ago(1)

    say_with_time "Converting #{violation_ids.size} violations to issues" do
      logins_by_id = User.all.inject({}) do |result, user|
        result[user.id]=user.login
        result
      end

      plans_by_id = ActionPlan.all.inject({}) do |result, plan|
        result[plan.id]=plan.kee
        result
      end

      violation_ids.each_slice(999) do |ids|
        violations = ActiveRecord::Base.connection.select_rows(sql_select_violation(ids))
        ActiveRecord::Base.transaction do
          violations.each do |violation|
            issue_key = new_key
            review_id = violation[0]
            created_at = violation[7] || one_year_ago
            resource_id = violation[1]
            if resource_id.present?
              issue = Issue.new(
                :kee => issue_key,
                :component_id => violation[1],
                :rule_id => violation[2],
                :severity => PRIORITY_TO_SEVERITY[violation[3].to_i] || 'MAJOR',
                :message => violation[4],
                :line => violation[5],
                :effort_to_fix => violation[6],
                :resolution => violation[13],
                :checksum => violation[8],
                :author_login => nil,
                :issue_attributes => violation[15],
                :issue_creation_date => created_at,
                :issue_close_date => nil,
                :created_at => created_at,
                :root_component_id => violation[17]
              )

              if review_id.present?
                # has review
                status = violation[11]
                issue.status=(status=='OPEN' ? 'CONFIRMED' : status)
                issue.issue_update_date=violation[16] || one_year_ago
                issue.updated_at=violation[16] || one_year_ago
                issue.severity=violation[12] || 'MAJOR'
                issue.manual_severity=violation[14]
                issue.reporter=logins_by_id[violation[9].to_i] if violation[9].present?
                issue.assignee=logins_by_id[violation[10].to_i] if violation[10].present?

                plan_id = select_plan_id(review_id)
                issue.action_plan_key=plans_by_id[plan_id.to_i] if plan_id

                review_comments = select_review_comments(review_id)
                review_comments.each do |review_comment|
                  user_id = review_comment[2]
                  login = logins_by_id[user_id.to_i]
                  if login
                    IssueChange.create(
                      :kee => new_key,
                      :issue_key => issue_key,
                      :user_login => login,
                      :change_type => 'comment',
                      :change_data => review_comment[3],
                      :created_at => review_comment[0],
                      :updated_at => review_comment[1]
                    )
                  end
                end

              else
                # does not have review
                issue.status='OPEN'
                issue.issue_update_date=created_at || one_year_ago
                issue.updated_at=created_at || one_year_ago
                issue.manual_severity=false
              end
              issue.save
            end
          end
        end
      end
    end

    drop_table('rule_failures')
    drop_table('reviews')
    drop_table('review_comments')
    drop_table('action_plans_reviews')
  end

  def self.truncate_issues
    ActiveRecord::Base.connection.execute('truncate table issues')
    ActiveRecord::Base.connection.execute('truncate table issue_changes')
  end

  def self.sql_select_violation(ids)
    "select rev.id, s.project_id, rf.rule_id, rf.failure_level, rf.message, rf.line, rf.cost, rf.created_at,
           rf.checksum,
           rev.user_id, rev.assignee_id, rev.status, rev.severity, rev.resolution, rev.manual_severity, rev.data,
           rev.updated_at, s.root_project_id
    from rule_failures rf
    inner join snapshots s on s.id=rf.snapshot_id
    left join reviews rev on rev.rule_failure_permanent_id=rf.permanent_id
    where rf.id in (#{ids.join(',')})"
  end

  def self.new_key
    Java::JavaUtil::UUID.randomUUID().toString()
  end

  def self.select_plan_id(review_id)
    ActiveRecord::Base.connection.select_value("select action_plan_id from action_plans_reviews where review_id=#{review_id}")
  end

  def self.select_review_comments(review_id)
    ActiveRecord::Base.connection.select_rows "select created_at, updated_at, user_id, review_text from review_comments where review_id=#{review_id}"
  end
end