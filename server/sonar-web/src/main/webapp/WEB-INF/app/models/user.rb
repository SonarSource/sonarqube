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
require 'digest/sha1'

class User < ActiveRecord::Base

  FAVOURITE_PROPERTY_KEY='favourite'

  has_and_belongs_to_many :groups

  has_many :user_roles, :dependent => :delete_all
  has_many :properties, :foreign_key => 'user_id', :dependent => :delete_all
  has_many :active_dashboards, :dependent => :destroy, :order => 'order_index'
  has_many :dashboards, :dependent => :destroy
  has_many :measure_filters, :class_name => 'MeasureFilter', :dependent => :delete_all, :order => 'name asc'

  # measure filters that are marked as favourites
  has_many :favourited_measure_filters, :class_name => 'MeasureFilter', :through => :measure_filter_favourites, :source => :measure_filter, :order => 'name asc'

  # the join table MEASURE_FILTER_FAVOURITES
  has_many :measure_filter_favourites, :class_name => 'MeasureFilterFavourite', :dependent => :delete_all

  include Authentication
  include Authentication::ByPassword
  include Authentication::ByCookieToken
  include NeedAuthorization::ForUser
  include NeedAuthentication::ForUser

  validates_presence_of :name
  validates_length_of :name, :maximum => 200, :unless => 'name.blank?'

  validates_length_of :email, :maximum => 100, :allow_blank => true, :allow_nil => true

  # The following two validations not needed, because they come with Authentication::ByPassword - see SONAR-2656
  #validates_length_of       :password, :within => 4..40, :if => :password_required?
  #validates_confirmation_of :password, :if => :password_required?

  validates_presence_of :login
  validates_length_of :login, :within => 2..255
  validates_uniqueness_of :login, :case_sensitive => true
  validates_format_of :login, :with => Authentication.login_regex, :message => Authentication.bad_login_message


  # HACK HACK HACK -- how to do attr_accessible from here?
  # prevents a user from submitting a crafted form that bypasses activation
  # anything else you want your user to change should be added here.
  attr_accessible :login, :email, :name, :password, :password_confirmation
  attr_accessor :token_authenticated

  ####
  # As now dates are saved in long they should be no more automatically managed by Rails
  ###

  def record_timestamps
    false
  end

  def before_create
    self.created_at = java.lang.System.currentTimeMillis
    self.updated_at = java.lang.System.currentTimeMillis
  end

  def before_save
    self.updated_at = java.lang.System.currentTimeMillis
  end

  def email=(value)
    write_attribute :email, (value && value.downcase)
  end

  # SCM accounts should also contain login and email
  def full_scm_accounts
    new_scm_accounts = self.scm_accounts.split(/\r?\n/).reject { |c| c.empty? } if self.scm_accounts
    new_scm_accounts = [] unless new_scm_accounts
    new_scm_accounts << self.login
    new_scm_accounts << self.email
    new_scm_accounts
  end

  def available_groups
    Group.all - self.groups
  end

  def set_groups(new_groups=[])
    self.groups.clear

    new_groups=(new_groups || []).compact.uniq
    self.groups = Group.find(new_groups)
    save
  end

  def <=>(other)
    return -1 if name.nil?
    return 1 if other.name.nil?
    name.downcase<=>other.name.downcase
  end

  # SONAR-3258
  def reactivate!(default_group_name)
    if default_group_name
      default_group=Group.find_by_name(default_group_name)
      self.groups<<default_group if default_group
    end
    self.active = true
    save!
  end

  def self.find_active_by_login(login)
    User.first(:conditions => ["login=:login AND active=:active", {:login => login, :active => true}])
  end


  #---------------------------------------------------------------------
  # USER PROPERTIES
  #---------------------------------------------------------------------
  def property(key)
    properties().each do |p|
      return p if (p.key==key)
    end
    nil
  end

  def property_value(key)
    prop=property(key)
    prop && prop.value
  end

  def set_property(options)
    key=options[:prop_key]
    prop=property(key)
    if prop
      prop.attributes=options
      prop.user_id=id
      prop.save!
    else
      prop=Property.new(options)
      prop.user_id=id
      properties<<prop
    end
  end
  
  #
  # This method is different from "set_property(options)" which can also add a new property:
  # it "really" adds a property and does not try to update a existing one with the same key.
  # This is used in the account controller to be able to add notification properties both on
  # a resource (resource_id != nil) or globally (resource_id = nil) - which was not possible
  # with "set_property(options)".
  #
  def add_property(options)
    prop=Property.new(options)
    prop.user_id=id
    properties<<prop
  end

  def delete_property(key)
    prop=property(key)
    if prop
      properties.delete(prop)
    end
  end

  def self.logins_to_ids(logins=[])
    if logins.size>0
      User.find(:all, :select => 'id', :conditions => ['login in (?)', logins]).map { |user| user.id }
    else
      []
    end
  end

  #---------------------------------------------------------------------
  # FAVOURITES
  #---------------------------------------------------------------------

  def favourite_ids
    @favourite_ids ||=
      begin
        properties().select { |p| p.key==FAVOURITE_PROPERTY_KEY }.map { |p| p.resource_id }
      end
    @favourite_ids
  end

  def favourites
    favourite_ids.size==0 ? [] : Project.find(:all, :conditions => ['id in (?) and enabled=?', favourite_ids, true])
  end

  def add_favourite(resource_key)
    favourite=Project.by_key(resource_key)
    if favourite
      delete_favourite(favourite.id)
      properties().create(:prop_key => FAVOURITE_PROPERTY_KEY, :user_id => id, :resource_id => favourite.id)
    end
    favourite
  end

  def delete_favourite(resource_key)
    rid=resource_key
    if resource_key.is_a?(String)
      resource=Project.by_key(resource_key)
      rid = resource.id if resource
    end
    if rid
      props=properties().select { |p| p.key==FAVOURITE_PROPERTY_KEY && p.resource_id==rid }
      if props.size>0
        properties().delete(props)
        return true
      end
    end
    false
  end

  def favourite?(resource_id)
    favourite_ids().include?(resource_id.to_i)
  end


  def notify_creation_handlers
    Java::OrgSonarServerUi::JRubyFacade.getInstance().onNewUser({'login' => self.login, 'name' => self.name, 'email' => self.email})
  end

  # Need to overwrite Authentication::ByPassword#password_required? for SONAR-4064  
  def password_required?
    (crypted_password.blank? && self.new_record?) || !password.blank?
  end

  def self.to_hash(java_user)
    hash = {:login => java_user.login, :name => java_user.name, :active => java_user.active}
    hash[:email] = java_user.email if java_user.email
    hash
  end

  def as_json(options={})
    {
      :login => login,
      :name => name,
      :email => email
    }
  end

  def to_hash
    hash = { :user => self }
    if errors and !errors.empty?
      hash[:errors] = errors.full_messages.map do |msg|
        { :msg => msg }
      end
    end
    hash
  end

end
