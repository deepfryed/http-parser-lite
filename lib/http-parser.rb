require 'http-parser/http_parser'

module HTTP
  class Parser
    CALLBACKS = %w(on_url on_header_field on_header_value on_body on_message_begin on_message_complete)

    CALLBACKS.each do |name|
      define_method(name) do |&block|
        raise ArgumentError, "block expected" unless block
        @callbacks[name.to_sym] = block
      end
    end

    def initialize
      @callbacks = {}
    end
  end
end
