# HTTP Parser Lite

A Lite&trade; wrapper around the Joyent http-parser goodness for Ruby

## Install

```
gem install http-parser-lite
```

## Example

```ruby
parser = HTTP::Parser.new

parser.on_message_begin do
  puts "message begin"
end

parser.on_message_complete do
  puts "message complete"
end

parser.on_url do |url|
  puts "url: #{url}"
end

parser.on_header_field do |name|
  puts "field: #{name}"
end

parser.on_header_value do |value|
  puts "value: #{value}"
end

parser.on_body do |body|
  puts "body: #{body}"
end

parser << "HTTP/1.1 200 OK\r\n"
parser << "Content-Type: text/plain;charset=utf-8\r\n"
parser << "Content-Length: 5\r\n"
parser << "Connection: close\r\n\r\n"
parser << "hello"

parser.reset

parser << "GET http://www.google.com/ HTTP/1.1\r\n\r\n"
```

## API

```
HTTP::Parser
    .new

    #reset

    #parse(data)
    #<<(data)

    #on_message_begin(&block)
    #on_message_complete(&block)
    #on_url(&block)
    #on_header_field(&block)
    #on_header_value(&block)
    #on_body(&block)

    #http_status
    #http_method
    #http_version
    #http_content_length
```

## License

[Creative Commons Attribution - CC BY](http://creativecommons.org/licenses/by/3.0)
