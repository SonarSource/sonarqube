#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2011 SonarSource
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
  has_many :review_comments, :order => "created_at", :dependent => :destroy
  alias_attribute :comments, :review_comments
  belongs_to :rule_failure, :foreign_key => 'rule_failure_permanent_id', :primary_key => 'permanent_id'
  
  validates_presence_of :user, :message => "can't be empty"
  validates_presence_of :status, :message => "can't be empty"
  
  before_save :assign_project
  
  STATUS_OPEN = 'OPEN'
  STATUS_RESOLVED = 'RESOLVED'
  STATUS_REOPENED = 'REOPENED'
  STATUS_CLOSED = 'CLOSED'
    
  def on_project?
    resource_id==project_id
  end
  
  def rule
    @rule ||= 
      begin
        rule_failure ? rule_failure.rule : nil
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
  def create_comment(params={})
    comments.create!(params)
    touch
  end

  def edit_comment(comment_id, comment_text)
    comment=comments.find(comment_id)
    if comment
      comment.text=comment_text
      comment.save!
      touch
    end
  end

  def edit_last_comment(comment_text)
    comment=comments.last
    comment.text=comment_text
    comment.save!
    touch
  end
  
  def delete_comment(comment_id)
    comment=comments.find(comment_id)
    comments.pop
    if comment
      comment.delete
      touch
    end
  end
  
  def reassign(user)
    self.assignee = user
    self.save!
  end
  
  # params are mandatory:
  # - :user (mandatory)
  # - :text (mandatory)
  # - :violation_id (optional: the violation to switch off - if not provided, a search will be done on rule_failure_permanent_id)
  def set_false_positive(is_false_positive, params={})
    if params[:user] && params[:text]
      if params[:violation_id]
        violation = RuleFailure.find(params[:violation_id])
        violation.switched_off=is_false_positive
        violation.save!
      else
        RuleFailure.find( :all, :conditions => [ "permanent_id = ?", self.rule_failure_permanent_id ] ).each do |violation|
          violation.switched_off=is_false_positive
          violation.save!
        end
      end
      create_comment(:user => params[:user], :text => params[:text])
      self.assignee = nil
      self.status = is_false_positive ? STATUS_RESOLVED : STATUS_REOPENED
      self.resolution = is_false_positive ? 'FALSE-POSITIVE' : nil
      self.save!
    end
  end
  
  def false_positive
    resolution == 'FALSE-POSITIVE'
  end
  
  def can_change_false_positive_flag?
    (status == STATUS_RESOLVED && resolution == 'FALSE-POSITIVE') || status == STATUS_OPEN || status == STATUS_REOPENED
  end

  def isResolved?
    status == STATUS_RESOLVED
  end
  
  def isClosed?
    status == STATUS_CLOSED
  end
  
  def isReopened?
    status == STATUS_REOPENED
  end
  
  def isOpen?
    status == STATUS_OPEN
  end
  
  def reopen
    self.status = STATUS_REOPENED
    self.resolution = nil
    self.save!
  end
  
  def resolve
    self.status = STATUS_RESOLVED
    self.resolution = 'FIXED'
    self.save!
  end
  
  
  
  #
  #
  # SEARCH METHODS
  #
  #  
  
  def self.search(options={})
    conditions=[]
    values={}
    
    # --- 'review_type' is deprecated since 2.9 ---
    # Following code just for backward compatibility
    review_type = options['review_type']
    if review_type
      if review_type == 'FALSE_POSITIVE'
        conditions << "resolution='FALSE-POSITIVE'"
      else
        conditions << "(resolution<>'FALSE-POSITIVE' OR resolution IS NULL)"
      end
    end
    # --- End of code for backward compatibility code ---

    # --- For UI
    false_positives = options['false_positives']
    if false_positives == "only"
      conditions << "resolution='FALSE-POSITIVE'"
    elsif false_positives == "without"
      conditions << "(resolution<>'FALSE-POSITIVE' OR resolution IS NULL)"
    end
    # --- End

    # --- For web-service
    resolutions = options['resolutions'].split(',') if options['resolutions']
    if resolutions && resolutions.size>0 && !resolutions[0].blank?
      conditions << 'resolution in (:resolutions)'
      values[:resolutions] = resolutions
    end
    # --- End

    ids=options['ids'].split(',') if options['ids']
    if options['id']
      conditions << 'id=:id'
      values[:id]=options['id'].to_i
    elsif ids && ids.size>0 && !ids[0].blank?
      conditions << 'id in (:ids)'
      values[:ids]=ids.map{|id| id.to_i}
    end

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
        values[:authors]=authors.map{|user_id| user_id.to_i}
      end
    end

    assignees=options['assignees'].split(',') if options['assignees']
    if assignees && assignees.size>0 && !assignees[0].blank?
      conditions << 'assignee_id in (:assignees)'
      unless Api::Utils.is_number?(assignees[0])
        values[:assignees]=User.logins_to_ids(assignees)
      else
        values[:assignees]=assignees.map{|user_id| user_id.to_i}
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
    
    Review.find(:all, :include => [ 'review_comments', 'project', 'assignee', 'resource', 'user' ], :conditions => [conditions.join(' AND '), values], :order => sort, :limit => 200)
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
      xml.resource(resource.kee)  if resource
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
    JSON(reviews.collect{|review| review.to_json(convert_markdown)})
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
