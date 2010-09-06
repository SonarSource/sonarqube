# Thai translation for Ruby on Rails
# original by Prem Sichanugrist (s@sikachu.com/sikandsak@gmail.com)
# activerecord keys fixed by Jittat Fakcharoenphol (jittat@gmail.com)

{ 
  :'th' => {
    :date => {
      :formats => {
        :default      => lambda { |date| "%d-%m-#{date.year+543}" },
        :short        => "%e %b",
        :long         => lambda { |date| "%e %B #{date.year+543}" },
        :long_ordinal => lambda { |date| "%e %B #{date.year+543}" },
        :only_day     => "%e"
      },
      :day_names => %w(อาทิตย์ จันทร์ อังคาร พุธ พฤหัสบดี ศุกร์ เสาร์),
      :abbr_day_names => %w(อา จ อ พ พฤ ศ ส),
      :month_names => [nil] + %w(มกราคม กุมภาพันธ์ มีนาคม เมษายน พฤษภาคม มิถุนายน กรกฎาคม สิงหาคม กันยายน ตุลาคม พฤศจิกายน ธันวาคม),
      :abbr_month_names => [nil] + %w(ม.ค. ก.พ. มี.ค. เม.ย. พ.ค. มิ.ย. ก.ค. ส.ค. ก.ย. ต.ค. พ.ย. ธ.ค.),
      :order => [:day, :month, :year]
    },
    :time => {
      :formats => {
        :default      => lambda { |time| "%a %d %b #{time.year+543} %H:%M:%S %Z" },
        :time         => "%H:%M น.",
        :short        => "%d %b %H:%M น.",
        :long         => lambda { |time| "%d %B #{time.year+543} %H:%M น." },
        :long_ordinal => lambda { |time| "%d %B #{time.year+543} %H:%M น." },
        :only_second  => "%S"
      },
      :time_with_zone => {
        :formats => {
          :default => lambda { |time| "%Y-%m-%d %H:%M:%S #{time.formatted_offset(false, 'UTC')}" }
        }
      },
      :am => '',
      :pm => ''
    },
    :datetime => {
      :formats => {
        :default => "%Y-%m-%dT%H:%M:%S%Z"
      },
      :distance_in_words => {
        :half_a_minute       => 'ครึ่งนาทีที่ผ่านมา',
        :less_than_x_seconds => 'น้อยกว่า {{count}} วินาที',
        :x_seconds           => '{{count}} วินาที',
        :less_than_x_minutes => 'น้อยกว่า {{count}} วินาที',
        :x_minutes           => '{{count}} นาที',
        :about_x_hours       => 'ประมาณ {{count}} ชั่วโมง',
        :x_hours             => '{{count}} ชั่วโมง',
        :about_x_days        => 'ประมาณ {{count}} วัน',
        :x_days              => '{{count}} วัน',
        :about_x_months      => 'ประมาณ {{count}} เดือน',
        :x_months            => '{{count}} เดือน',
        :about_x_years       => 'ประมาณ {{count}} ปี',
        :over_x_years        => 'เกิน {{count}} ปี'
      }
    },

    # numbers
    :number => {
      :format => {
        :precision => 3,
        :separator => '.',
        :delimiter => ','
      },
      :currency => {
        :format => {
          :unit => 'Baht',
          :precision => 2,
          :format => '%n %u'
        }
      },
      :human => {
        :format => {
          :precision => 1,
          :delimiter => ''
        },
       :storage_units => {
         :format => "%n %u",
         :units => {
           :byte => "B",
           :kb   => "KB",
           :mb   => "MB",
           :gb   => "GB",
           :tb   => "TB",
         }
       }
      },
    },

    # Active Record
    :activerecord => {
      :errors => {
        :template => {
          :header => {
            :one => "ไม่สามารถบันทึก {{model}} ได้เนื่องจากเกิดข้อผิดพลาด",
            :other => "ไม่สามารถบันทึก {{model}} ได้เนื่องจากเกิด {{count}} ข้อผิดพลาด"
          },
          :body => "โปรดตรวจสอบข้อมูลที่คุณกรอกในช่องต่อไปนี้:"
        },
        :messages => {
          :inclusion => "ไม่ได้อยู่ในลิสต์",
          :exclusion => "ถูกจองเอาไว้แล้ว",
          :invalid => "ไม่ถูกต้อง",
          :confirmation => "ไม่ตรงกับการยืนยัน",
          :accepted  => "ต้องอยู่ในรูปแบบที่ยอมรับ",
          :empty => "ต้องไม้เว้นว่างเอาไว้",
          :blank => "ต้องไม่เว้นว่างเอาไว้",
          :too_long => "ยาวเกินไป (ต้องไม่เกิน {{count}} ตัวอักษร)",
          :too_short => "สั้นเกินไป (ต้องยาวกว่า {{count}} ตัวอักษร)",
          :wrong_length => "มีความยาวไม่ถูกต้อง (ต้องมีความยาว {{count}} ตัวอักษร)",
          :taken => "ถูกใช้ไปแล้ว",
          :not_a_number => "ไม่ใช่ตัวเลข",
          :greater_than => "ต้องมากกว่า {{count}}",
          :greater_than_or_equal_to => "ต้องมากกว่าหรือเท่ากับ {{count}}",
          :equal_to => "ต้องเท่ากับ {{count}}",
          :less_than => "ต้องน้อยกว่า {{count}}",
          :less_than_or_equal_to => "ต้องน้อยกว่าหรือเท่ากับ {{count}}",
          :odd => "ต้องเป็นเลขคี่",
          :even => "ต้องเป็นเลขคู่"
        }
      }
    }
  }
} 

