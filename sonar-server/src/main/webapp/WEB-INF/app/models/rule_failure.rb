#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
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
# You should have received a copy of the GNU Lesser General Public
# License along with {library}; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#

class RuleFailure < ActiveRecord::Base

  belongs_to :rule
  belongs_to :snapshot
  has_one :review, :primary_key => 'permanent_id', :foreign_key => 'rule_failure_permanent_id', :order => 'created_at'
  after_save :update_permanent_id
  validates_presence_of :rule, :snapshot

  # first line of message
  def title
    @title||=
      begin
        if message.blank?
          rule.name
        else
          parts=Api::Utils.split_newlines(message)
          parts.size==0 ? rule.name : parts[0]
        end
      end
  end

  def plain_message
    @plain_message ||=
      begin
        Api::Utils.convert_string_to_unix_newlines(message)
      end
  end

  def html_message
    @html_message ||=
      begin
        message ? Api::Utils.split_newlines(ERB::Util.html_escape(message)).join('<br/>') : ''
      end
  end

  def severity
    Sonar::RulePriority.to_s(failure_level)
  end

  def resource
    snapshot.resource
  end

  def to_json(include_review=false, convert_markdown=false)
    json = {}
    json['id'] = id
    json['message'] = plain_message if plain_message
    json['line'] = line if line && line>=1
    json['priority'] = severity
    json['switchedOff']=true if switched_off?
    if created_at
      json['createdAt'] = Api::Utils.format_datetime(created_at)
    end
    json['rule'] = {
      :key => rule.key,
      :name => rule.name
    }
    json['resource'] = {
      :key => resource.key,
      :name => resource.name,
      :scope => resource.scope,
      :qualifier => resource.qualifier,
      :language => resource.language
    }
    json['review'] = review.to_json(convert_markdown) if include_review && review
    json
  end

  def to_xml(xml=Builder::XmlMarkup.new(:indent => 0), include_review=false, convert_markdown=false)
    xml.violation do
      xml.id(id)
      xml.message(plain_message) if plain_message
      xml.line(line) if line && line>=1
      xml.priority(severity)
      xml.switchedOff(true) if switched_off?
      if created_at
        xml.createdAt(Api::Utils.format_datetime(created_at))
      end
      xml.rule do
        xml.key(rule.key)
        xml.name(rule.name)
      end
      xml.resource do
        xml.key(resource.key)
        xml.name(resource.name)
        xml.scope(resource.scope)
        xml.qualifier(resource.qualifier)
        xml.language(resource.language)
      end
      review.to_xml(xml, convert_markdown) if include_review && review
    end
  end

  def build_review(options={})
    if self.review.nil?
      self.review=Review.new(
        {
          :status => Review::STATUS_OPEN,
          :severity => severity,
          :resource_line => line,
          :resource => resource,
          :title => title,
          :rule => rule,
          :manual_violation => false,
          :manual_severity => false
        }.merge(options))
    end
  end

  def create_review!(options={})
    build_review(options)
    self.review.save!
  end

  # Options :
  # - snapshot_id (integer)
  # - resource_id (integer)
  # - ancestor_resource_id (integer)
  # - ancestor_snapshot (Snapshot)
  # - created_after (datetime)
  # - switched_off (boolean)
  # - review_statuses (array of strings, can include nil)
  # - review_assignee_id (integer, nil means no assignee)
  # - severity
  # - rule_id
  # - planned (boolean)
  #
  # WARNING: association 'snapshot' is not loaded
  #
  def self.search(options={})
    conditions=[]
    values=[]
    includes=[:rule, {:review => :action_plans}]

    if options.has_key? :snapshot_id
      conditions << 'rule_failures.snapshot_id = ?'
      values << options[:snapshot_id].to_i
    end

    if options.has_key? :resource_id
      conditions << 'rule_failures.snapshot_id in (select id from snapshots where islast=? and status=? and project_id=?)'
      values << true
      values << 'P'
      values << options[:resource_id].to_i
    end

    if options.has_key? :ancestor_resource_id
      ancestor = Snapshot.find(:first, :select => 'id,path', :conditions => {:islast => true, :status => 'P', :project_id => options[:ancestor_resource_id].to_i})
      options[:ancestor_snapshot]=ancestor
    end

    if options.has_key? :ancestor_snapshot
      ancestor_snapshot=options[:ancestor_snapshot]
      if ancestor_snapshot
        conditions << 'rule_failures.snapshot_id in (select id from snapshots where islast=? and status=? and (id=? or (root_snapshot_id=? and path like ?)))'
        values << true
        values << 'P'
        values << ancestor_snapshot.id
        values << ancestor_snapshot.id
        values << "#{ancestor_snapshot.path}#{ancestor_snapshot.id}.%"
      else
        return []
      end
    end

    if options.has_key? :rule_id
      conditions << 'rule_failures.rule_id=?'
      values << options[:rule_id].to_i
    end

    if options.has_key? :created_after
      conditions << 'rule_failures.created_at>?'
      values << options[:created_after]
    end

    if options[:switched_off]
      conditions << 'rule_failures.switched_off=?'
      values << true
    else
      conditions << '(rule_failures.switched_off is null or rule_failures.switched_off = ?)'
      values << false
    end

    if options.has_key? :review_statuses
      statuses = options[:review_statuses]
      unless statuses.empty?
        if statuses.include? nil
          if statuses.size==1
            # only nil : unreviewed violations
            conditions << 'not exists(select id from reviews where rule_failure_permanent_id=rule_failures.permanent_id)'
          else
            conditions << '(reviews.status in (?) or not exists(select id from reviews where rule_failure_permanent_id=rule_failures.permanent_id))'
            values << options[:review_statuses].compact
          end
        else
          conditions << 'reviews.status in (?)'
          values << options[:review_statuses]
        end
      end
    end

    if options.has_key? :review_assignee_id
      review_assignee_id = options[:review_assignee_id]
      if review_assignee_id
        conditions << 'reviews.assignee_id=?'
        values << review_assignee_id.to_i
      else
        conditions << '(reviews.assignee_id is null or not exists(select id from reviews where rule_failure_permanent_id=rule_failures.permanent_id))'
      end
    end

    if options.has_key? :severity
      conditions << 'failure_level=?'
      values << Sonar::RulePriority.id(options[:severity])
    end

    result = find(:all, :include => includes, :conditions => [conditions.join(' and ')] + values, :order => 'rule_failures.failure_level DESC')

    if options.has_key? :planned
      # this condition can not be implemented with SQL
      if options[:planned]
        result = result.select { |violation| violation.review && violation.review.planned? }
      else
        result = result.reject { |violation| violation.review && violation.review.planned? }
      end
    end

    result
  end


  #
  # Constraint : all the violations are in the same project
  #
  def self.available_java_screens_for_violations(violations, resource, user)
    reviews = violations.map { |violation| to_java_workflow_review(violation) }
    context = to_java_workflow_context(resource, user)
    Java::OrgSonarServerUi::JRubyFacade.getInstance().listAvailableReviewsScreens(reviews, context)
  end

  def available_java_screens(user)
    if user
      review = RuleFailure.to_java_workflow_review(self)
      context = RuleFailure.to_java_workflow_context(snapshot.root_snapshot.project, user)
      Java::OrgSonarServerUi::JRubyFacade.getInstance().listAvailableReviewScreens(review, context)
    else
      []
    end
  end

  def self.execute_command(command_key, violation, user, parameters)
    review = to_java_workflow_review(violation)
    context = to_java_workflow_context(violation.resource, user)
    Java::OrgSonarServerUi::JRubyFacade.getInstance().executeReviewCommand(command_key, review, context, parameters)
  end

  def self.to_java_workflow_review(violation)
    java_review=Java::OrgSonarApiWorkflowInternal::DefaultReview.new
    java_review.setViolationId(violation.id)
    java_review.setSeverity(violation.severity.to_s)
    java_review.setRuleKey(violation.rule.plugin_rule_key)
    java_review.setRuleRepositoryKey(violation.rule.repository_key)
    java_review.setRuleName(violation.rule.name(false)) # rule name is not localized
    java_review.setSwitchedOff(violation.switched_off||false)
    java_review.setMessage(violation.message)
    java_review.setLine(violation.line)

    review = violation.review
    if review
      java_review.setReviewId(review.id)
      java_review.setStatus(review.status)
      java_review.setResolution(review.resolution)
      java_review.setManual(review.manual_violation)
      java_review.setPropertiesAsString(review.data)
    else
      java_review.setStatus(Review::STATUS_IDLE)
    end
    java_review
  end

  def self.to_java_workflow_context(resource, user)
    java_context = Java::OrgSonarApiWorkflowInternal::DefaultWorkflowContext.new
    java_context.setUserId(user.id)
    java_context.setUserLogin(user.login)
    java_context.setUserName(user.name)
    java_context.setUserEmail(user.email)
    java_context.setIsAdmin(user.has_role?(:admin))
    java_context.setProjectId(resource.root_project.id)
    java_context
  end

  private
  def update_permanent_id
    if self.permanent_id.nil? && self.id
      self.permanent_id = self.id
      save!
    end
  end

end
