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
  belongs_to :rule_failure, :foreign_key => 'rule_failure_permanent_id'
  
  validates_presence_of :user, :message => "can't be empty"
  validates_presence_of :title, :message => "can't be empty"
  validates_presence_of :review_type, :message => "can't be empty"
  validates_presence_of :status, :message => "can't be empty"
  
  before_save :assign_project
  
  TYPE_COMMENTS = "comments"
  TYPE_FALSE_POSITIVE = "f-positive"
  
  STATUS_OPEN = "open"
  STATUS_CLOSED = "closed"
  
  def self.default_severity
    return Severity::MAJOR
  end
  
  def self.default_type
    return TYPE_COMMENTS
  end

  def self.default_status
    return STATUS_OPEN
  end
    
  def source?
    resource!=nil && resource.last_snapshot!=nil && resource.last_snapshot.source!=nil
  end

  private
  
  def assign_project
    if self.project.nil? && self.resource
      self.project=self.resource.project
    end
  end
end
