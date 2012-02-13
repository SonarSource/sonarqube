#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#
class Review < ActiveRecord::Base
  belongs_to :user
  belongs_to :assignee, :class_name => "User", :foreign_key => "assignee_id"
  belongs_to :resource, :class_name => "Project", :foreign_key => "resource_id"
  belongs_to :project, :class_name => "Project", :foreign_key => "project_id"
  belongs_to :rule
  has_many :review_comments, :order => "created_at", :dependent => :destroy
  alias_attribute :comments, :review_comments
  has_and_belongs_to_many :action_plans

  validates_presence_of :status, :message => "can't be empty"
  validates_inclusion_of :severity, :in => Severity::KEYS

  before_save :assign_project

  STATUS_OPEN = 'OPEN'
  STATUS_RESOLVED = 'RESOLVED'
  STATUS_REOPENED = 'REOPENED'
  STATUS_CLOSED = 'CLOSED'

  RESOLUTION_FALSE_POSITIVE = 'FALSE-POSITIVE'
  RESOLUTION_FIXED = 'FIXED'

  def on_project?
    resource_id==project_id
  end

  def rule
    @rule ||=
      begin
        rule_failure ? rule_failure.rule : nil
      end
  end

  def violation
    rule_failure
  end

  def rule_failure
    @rule_failure ||=
      begin
        # We need to manually run this DB request as the real relation Reviews-RuleFailures is 1:n but we want only 1 violation
        # (more than 1 violation can have the same "permanent_id" when several analyses are run in a small time frame)
        RuleFailure.find(:first, :conditions => {:permanent_id => rule_failure_permanent_id}, :order => 'id desc')
      end
  end


  #
  #
  # REVIEW CORE METHODS
  #
  #

  # params are mandatory:
  # - :user
  # - :text
  def create_comment(options={})
    comment = comments.create!(options)
    touch
    notification_manager.notifyChanged(id.to_i, comment.user.login.to_java, to_java_map, to_java_map("comment" => comment.text))
  end

  def edit_comment(current_user, comment_id, comment_text)
    comment=comments.find(comment_id)
    if comment
      old_comment_text=comment.text
      comment.text=comment_text
      comment.save!
      touch
      notification_manager.notifyChanged(id.to_i, current_user.login.to_java, to_java_map("comment" => old_comment_text), to_java_map("comment" => comment.text))
    end
  end

  # TODO Godin: seems that this method not used anymore
  def edit_last_comment(current_user, comment_text)
    comment=comments.last
    old_comment_text=comment.text
    comment.text=comment_text
    comment.save!
    touch
    notification_manager.notifyChanged(id.to_i, current_user.login.to_java, to_java_map("comment" => old_comment_text), to_java_map("comment" => comment.text))
  end

  def delete_comment(current_user, comment_id)
    comment=comments.find(comment_id)
    comments.pop
    if comment
      old_comment_text=comment.text
      comment.delete
      touch
      notification_manager.notifyChanged(id.to_i, current_user.login.to_java, to_java_map("comment" => old_comment_text), to_java_map)
    end
  end

  def notification_manager
    Java::OrgSonarServerUi::JRubyFacade.getInstance().getReviewsNotificationManager()
  end

  def to_java_map(options = {})
    java.util.HashMap.new(
      {
        "project" => project.long_name.to_java,
        "resource" => resource.long_name.to_java,
        "title" => title.to_java,
        "creator" => user == nil ? nil : user.login.to_java,
        "assignee" => assignee == nil ? nil : assignee.login.to_java,
        "status" => status.to_java,
        "resolution" => resolution.to_java,
        "severity" => severity.to_java
      }.merge(options))
  end

  def reassign(current_user, assignee, options={})
    if options[:text].present?
      comments.create!(:user => current_user, :text => options[:text])
    end
    old = self.to_java_map
    self.assignee = assignee
    self.save!
    notification_manager.notifyChanged(id.to_i, current_user.login.to_java, old, to_java_map)
  end

  def reopen(current_user, options={})
    old = self.to_java_map
    if options[:text].present?
      comments.create!(:user => current_user, :text => options[:text])
    end
    self.status = STATUS_REOPENED
    self.resolution = nil
    self.save!
    notification_manager.notifyChanged(id.to_i, current_user.login.to_java, old, to_java_map)
  end

  def resolve(current_user, options={})
    old = self.to_java_map
    if options[:text].present?
      comments.create!(:user => current_user, :text => options[:text])
    end
    self.status = STATUS_RESOLVED
    self.resolution = RESOLUTION_FIXED
    self.save!
    notification_manager.notifyChanged(id.to_i, current_user.login.to_java, old, to_java_map)
  end

  # Parameters:
  # - :text
  def set_false_positive(is_false_positive, user, options={})
    if violation.nil?
      bad_request('This review does not relate to a violation')
    end
    violation.switched_off=is_false_positive
    violation.save!
    if options[:text].present?
      comments.create!(:user => user, :text => options[:text])
    end
    old = self.to_java_map
    self.assignee = nil
    self.status = is_false_positive ? STATUS_RESOLVED : STATUS_REOPENED
    self.resolution = is_false_positive ? RESOLUTION_FALSE_POSITIVE : nil
    self.save!
    notification_manager.notifyChanged(id.to_i, user.login.to_java, old, to_java_map("comment" => options[:text]))
  end

  def false_positive
    resolution == RESOLUTION_FALSE_POSITIVE
  end

  def can_change_false_positive_flag?
    (status == STATUS_RESOLVED && resolution == RESOLUTION_FALSE_POSITIVE) || status == STATUS_OPEN || status == STATUS_REOPENED
  end

  def set_severity(new_severity, user, options={})
    if options[:text].present?
      comments.create!(:user => user, :text => options[:text])
    end
    old = self.to_java_map
    self.severity=new_severity
    self.manual_severity=(new_severity!=violation.severity)
    self.save!
    notification_manager.notifyChanged(id.to_i, user.login.to_java, old, to_java_map("comment" => options[:text]))
  end

  def link_to_action_plan(action_plan, user, options={})
    if options[:text].present?
      comments.create!(:user => user, :text => options[:text])
    end
    old = self.to_java_map
    self.action_plans.clear
    if action_plan
      self.action_plans << action_plan
    end
    self.save!
    notification_manager.notifyChanged(id.to_i, user.login.to_java, old, to_java_map("action_plans" => action_plan ? action_plan.name : ''))
  end

  def resolved?
    status == STATUS_RESOLVED
  end

  def closed?
    status == STATUS_CLOSED
  end

  def reopened?
    status == STATUS_REOPENED
  end

  def open?
    status == STATUS_OPEN
  end

  def active?
    status == STATUS_OPEN || status == STATUS_REOPENED
  end
  
  def linked_to? (action_plan)
    action_plans.include? action_plan
  end
  
  def planned?
    action_plans.size!=0
  end
  
  def assigned?
    assignee_id != nil
  end
  
  # used as long as we currently allow to link a review to only 1 action plan.
  def action_plan
    action_plans[0]
  end

  #
  #
  # SEARCH METHODS
  #
  #  

  def self.search(options={})
    conditions=[]
    values={}

    if options['id'].present?
      conditions << 'id=:id'
      values[:id]=options['id'].to_i
    elsif options['ids'].present?
      ids=options['ids'].split(',')
      conditions << 'id in (:ids)'
      values[:ids]=ids.map { |id| id.to_i }
    else
      
      # --- 'review_type' is deprecated since 2.9 ---
      # Following code just for backward compatibility
      review_type = options['review_type']
      if review_type
        if review_type == RESOLUTION_FALSE_POSITIVE
          conditions << "resolution='#{RESOLUTION_FALSE_POSITIVE}'"
        else
          conditions << "(resolution<>'#{RESOLUTION_FALSE_POSITIVE}' OR resolution IS NULL)"
        end
      end
      # --- End of code for backward compatibility code ---

      # --- For UI
      false_positives = options['false_positives']
      if false_positives == "only"
        conditions << "resolution='#{RESOLUTION_FALSE_POSITIVE}'"
      elsif false_positives == "without"
        conditions << "(resolution<>'#{RESOLUTION_FALSE_POSITIVE}' OR resolution IS NULL)"
      end
      # --- End

      # --- For web-service
      resolutions = options['resolutions'].split(',') if options['resolutions']
      if resolutions && resolutions.size>0 && !resolutions[0].blank?
        conditions << 'resolution in (:resolutions)'
        values[:resolutions] = resolutions
      end
      # --- End


      projects=options['projects'].split(',') if options['projects']
      if projects && projects.size>0 && !projects[0].blank?
        conditions << 'project_id in (:projects)'
        projectIds = []
        projects.each do |project|
          foundProject = Project.by_key(project)
          projectIds << foundProject.id if foundProject
        end
        values[:projects]=projectIds
      end

      resources=options['resources'].split(',') if options['resources']
      if resources && resources.size>0 && !resources[0].blank?
        conditions << 'resource_id in (:resources)'
        resourceIds = []
        resources.each do |resource|
          foundResource = Project.by_key(resource)
          resourceIds << foundResource.id if foundResource
        end
        values[:resources]=resourceIds
      end

      statuses=options['statuses'].split(',') if options['statuses']
      if statuses && statuses.size>0 && !statuses[0].blank?
        conditions << 'status in (:statuses)'
        values[:statuses]=statuses
      end

      severities=options['severities'].split(',') if options['severities']
      if severities && severities.size>0 && !severities[0].blank?
        conditions << 'severity in (:severities)'
        values[:severities]=severities
      end

      authors=options['authors'].split(',') if options['authors']
      if authors && authors.size>0 && !authors[0].blank?
        conditions << 'user_id in (:authors)'
        unless Api::Utils.is_number?(authors[0])
          values[:authors]=User.logins_to_ids(authors)
        else
          values[:authors]=authors.map { |user_id| user_id.to_i }
        end
      end

      assignees=options['assignees'].split(',') if options['assignees']
      if assignees
        if assignees.size == 0
          # Unassigned reviews
          conditions << 'assignee_id IS NULL'
        else
          # Assigned reviews
          conditions << 'assignee_id in (:assignees)'
          unless Api::Utils.is_number?(assignees[0])
            values[:assignees]=User.logins_to_ids(assignees)
          else
            values[:assignees]=assignees.map { |user_id| user_id.to_i }
          end
        end
      end

      action_plan_id = options['action_plan_id']
      if action_plan_id
        action_plan = ActionPlan.find action_plan_id.to_i, :include => 'reviews'
        if action_plan && action_plan.reviews.size>0
          conditions << 'id in (:ids)'
          values[:ids]=action_plan.reviews.map { |r| r.id }
        else
          # no action plan or action plan is empty => no need to look into the database
          no_need_for_db_request = true
        end
      elsif options['unplanned']
        conditions << 'id not in (:ids)'
        values[:ids]=find_by_sql('SELECT DISTINCT review_id as id from action_plans_reviews')
      end

      from=options['from']
      if from
        conditions << 'created_at >= :from'
        values[:from] = from
      end

      to=options['to']
      if from
        conditions << 'created_at <= :to'
        values[:to] = to
      end
    end

    sort=options['sort']
    asc=options['asc']
    if sort
      if asc
        sort += ' ASC, reviews.updated_at DESC'
      else
        sort += ' DESC, reviews.updated_at DESC'
      end
    else
      sort = 'reviews.updated_at DESC'
    end

    found_reviews = []
    found_reviews = Review.find(:all, :include => ['review_comments', 'project', 'assignee', 'resource', 'user'], :conditions => [conditions.join(' AND '), values], :order => sort, :limit => 200) unless no_need_for_db_request
    found_reviews
  end


  #
  #
  # XML AND JSON UTILITY METHODS
  #
  #

  def self.reviews_to_xml(reviews, convert_markdown=false)
    xml = Builder::XmlMarkup.new(:indent => 0)
    xml.instruct!
    xml.reviews do
      reviews.each do |review|
        review.to_xml(xml, convert_markdown)
      end
    end
  end

  def to_xml(xml, convert_markdown=false)
    xml.review do
      xml.id(id.to_i)
      xml.createdAt(Api::Utils.format_datetime(created_at))
      xml.updatedAt(Api::Utils.format_datetime(updated_at))
      xml.author(user.login)
      xml.assignee(assignee.login) if assignee
      xml.title(title)
      xml.status(status)
      xml.resolution(resolution) if resolution
      xml.severity(severity)
      xml.resource(resource.kee) if resource
      xml.line(resource_line) if resource_line && resource_line>0
      xml.violationId(rule_failure_permanent_id) if rule_failure_permanent_id
      xml.comments do
        review_comments.each do |comment|
          xml.comment do
            xml.id(comment.id.to_i)
            xml.author(comment.user.login)
            xml.updatedAt(Api::Utils.format_datetime(comment.updated_at))
            if convert_markdown
              xml.text(comment.html_text)
            else
              xml.text(comment.plain_text)
            end
          end
        end
      end
    end
  end

  def self.reviews_to_json(reviews, convert_markdown=false)
    JSON(reviews.collect { |review| review.to_json(convert_markdown) })
  end

  def to_json(convert_markdown=false)
    json = {}
    json['id'] = id.to_i
    json['createdAt'] = Api::Utils.format_datetime(created_at)
    json['updatedAt'] = Api::Utils.format_datetime(updated_at)
    json['author'] = user.login
    json['assignee'] = assignee.login if assignee
    json['title'] = title if title
    json['status'] = status
    json['resolution'] = resolution if resolution
    json['severity'] = severity
    json['resource'] = resource.kee if resource
    json['line'] = resource_line if resource_line && resource_line>0
    json['violationId'] = rule_failure_permanent_id if rule_failure_permanent_id
    comments = []
    review_comments.each do |comment|
      comments << {
        'id' => comment.id.to_i,
        'author' => comment.user.login,
        'updatedAt' => Api::Utils.format_datetime(comment.updated_at),
        'text' => convert_markdown ? comment.html_text : comment.plain_text
      }
    end
    json['comments'] = comments
    json
  end

  #
  #
  # PRIVATE METHODS
  #
  #
  private

  def assign_project
    if self.project.nil? && self.resource
      self.project=self.resource.root_project
    end
  end


end
