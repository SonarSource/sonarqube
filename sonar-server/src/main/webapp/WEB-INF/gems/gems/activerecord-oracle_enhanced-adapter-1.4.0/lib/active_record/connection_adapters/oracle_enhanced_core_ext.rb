require "bigdecimal"
if (BigDecimal.instance_methods & ["to_d", :to_d]).empty?
  BigDecimal.class_eval do
    def to_d #:nodoc:
      self
    end
  end
end

if (Bignum.instance_methods & ["to_d", :to_d]).empty?
  Bignum.class_eval do
    def to_d #:nodoc:
      BigDecimal.new(self.to_s)
    end
  end
end

if (Fixnum.instance_methods & ["to_d", :to_d]).empty?
  Fixnum.class_eval do
    def to_d #:nodoc:
      BigDecimal.new(self.to_s)
    end
  end
end
