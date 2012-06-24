require 'helper'

describe 'http-parser' do
  attr_reader :parser

  before do
    @parser = HTTP::Parser.new
  end

  it 'should parse request' do
    got = []
    parser.on_url          {|data| got << data}
    parser.on_header_field {|data| got << data}
    parser.on_header_value {|data| got << data}

    parser << "GET http://www.google.com HTTP/1.1\r\nX-Test-Key: 1\r\n\r\n"
    assert_equal %w(http://www.google.com X-Test-Key 1), got
    assert_equal 'GET', parser.http_method
    assert_equal '1.1', parser.http_version
  end

  it 'should parse response' do
    got = []
    parser.on_url          {|data| got << data}
    parser.on_header_field {|data| got << data}
    parser.on_header_value {|data| got << data}
    parser.on_body         {|data| got << data}
    parser << "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: 5\r\n\r\nhello"

    assert_equal %w(Content-Type text/plain Content-Length 5 hello), got
    assert_equal '1.1', parser.http_version
    assert_equal 200,   parser.http_status
  end


  it 'should call callbacks' do
    got = []
    parser.on_message_begin    {got << 's'}
    parser.on_message_complete {got << 'e'}
    parser.on_url              {got << 'u'}
    parser.on_header_field     {got << 'f'}
    parser.on_header_value     {got << 'v'}
    parser.on_body             {got << 'b'}

    parser << "POST / HTTP/1.0\r\nContent-Type: text/plain\r\nContent-Length: 5\r\n\r\nhello"
    assert_equal %w(s u f v f v b e), got
  end

  it 'should parse chunked response' do
    got = []
    parser.on_url          {|data| got << data}
    parser.on_header_field {|data| got << data}
    parser.on_header_value {|data| got << data}
    parser.on_body         {|data| got << data}
    parser << "HTTP/1.1 404 OK\r\nContent-Type: text/plain\r\nTransfer-Encoding: chunked\r\n\r\n"
    parser << "8\r\n"
    parser << "document\r\n"
    parser << "3\r\n"
    parser << "not\r\n"
    parser << "5\r\n"
    parser << "found\r\n"
    parser << "0\r\n"

    assert_equal 404, parser.http_status
    assert_equal %w(Content-Type text/plain Transfer-Encoding chunked document not found), got
  end

  it 'should parse CONNECT' do
    got = []
    parser.on_url          {|data| got << data}
    parser.on_header_field {|data| got << data}
    parser.on_header_value {|data| got << data}
    parser << "CONNECT 345.example.com:443 HTTP/1.1\r\nUser-Agent: X-Test-UA\r\n\r\n"

    assert_equal %w(345.example.com:443 User-Agent X-Test-UA), got
    assert_equal 'CONNECT', parser.http_method
    assert_equal '1.1',     parser.http_version
  end

  it 'should raise an error on invalid data' do
    assert_raises(HTTP::Parser::Error) { parser << "GET / foobar\r\n" }
  end

  it 'should pause parser' do
    got = []
    parser.on_url {|data| got << data; parser.pause }

    assert_raises(HTTP::Parser::Error) do
      parser << "GET /hello HTTP/1.0\r\n\r\n"
    end

    assert_equal %w(/hello), got
  end

  it 'should resume parser' do
    got = []
    parser.on_url          {|data| got << data; parser.pause}
    parser.on_header_field {|data| got << data}
    parser.on_header_value {|data| got << data}

    parser << "GET /hello "
    assert parser.paused?
    parser.resume
    parser << "HTTP/1.0\r\n"
    parser << "X-Test-Field: 1\r\n\r\n"

    assert %w(/hello X-Test-Field 1), got
  end
end
