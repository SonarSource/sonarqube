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
  belongs_to :rule_failure
  belongs_to :resource, :class_name => "Project", :foreign_key => "resource_id"
  has_many :review_comments, :order => "created_at", :dependent => :destroy
  validates_presence_of :user, :message => "can't be empty"
  validates_presence_of :review_type, :message => "can't be empty"
  validates_presence_of :status, :message => "can't be empty"

  SEVERITY_INFO = "info"
  SEVERITY_MINOR = "minor"
  SEVERITY_MAJOR = "major"
  SEVERITY_CRITICAL = "critical"
  SEVERITY_BLOCKER = "blocker"
  
  TYPE_COMMENTS = "comments"
  TYPE_FALSE_POSITIVE = "f-positive"
  
  STATUS_OPEN = "open"
  STATUS_CLOSED = "closed"


  def self.default_severity
    return SEVERITY_MAJOR
  end
  
  def self.default_type
    return TYPE_COMMENTS
  end

  def self.default_status
    return STATUS_OPEN
  end
  
  def self.severity_options
    severity_ops = []
    severity_ops << ["Info", SEVERITY_INFO]
    severity_ops << ["Minor", SEVERITY_MINOR]
    severity_ops << ["Major", SEVERITY_MAJOR]
    severity_ops << ["Critical", SEVERITY_CRITICAL]
    severity_ops << ["Blocker", SEVERITY_BLOCKER]
    return severity_ops
  end

end
