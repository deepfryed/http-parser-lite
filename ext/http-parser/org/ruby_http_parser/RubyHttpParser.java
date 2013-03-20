package org.ruby_http_parser;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubySymbol;

import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;

import org.jruby.util.ByteList;

import org.jcodings.specific.UTF8Encoding;

import java.nio.ByteBuffer;
import http_parser.*;
import http_parser.lolevel.ParserSettings;
import http_parser.lolevel.HTTPCallback;
import http_parser.lolevel.HTTPDataCallback;

public class RubyHttpParser extends RubyObject {

  public static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
    public IRubyObject allocate(Ruby runtime, RubyClass klass) {
      return new RubyHttpParser(runtime, klass);
    }
  };

  byte[] fetchBytes (ByteBuffer b, int pos, int len) {
    byte[] by = new byte[len];
    int saved = b.position();
    b.position(pos);
    b.get(by);
    b.position(saved);
    return by;
  }

  public class StopException extends RuntimeException {
  }

  private Ruby runtime;
  private HTTPParser parser;
  private ParserSettings settings;

  private RubyClass eParserError;

  private RubyHash headers;

  private Block on_url;
  private Block on_status_complete;
  private Block on_header_field;
  private Block on_header_value;
  private Block on_body;
  private Block on_message_begin;
  private Block on_message_complete;
  private Block on_headers_complete;

  private IRubyObject requestUrl;
  private IRubyObject requestPath;
  private IRubyObject queryString;
  private IRubyObject fragment;

  private IRubyObject header_value_type;
  private IRubyObject upgradeData;

  private IRubyObject callback_object;

  private byte[] _current_header;
  private byte[] _last_header;

  private boolean pause;
  private String error;

  public RubyHttpParser(final Ruby runtime, RubyClass clazz) {
    super(runtime,clazz);

    this.runtime = runtime;
    this.eParserError = (RubyClass)runtime.getModule("HTTP").getClass("Parser").getConstant("Error");

    this.on_url = null;
    this.on_status_complete = null;
    this.on_header_field = null;
    this.on_header_value = null;
    this.on_body = null;
    this.on_message_begin = null;
    this.on_message_complete = null;
    this.on_headers_complete = null;

    this.callback_object = null;

    this.header_value_type = runtime.getModule("HTTP").getClass("Parser").getInstanceVariable("@default_header_value_type");

    initSettings();
    init();
  }

  private void initSettings() {
    this.settings = new ParserSettings();

    this.settings.on_path = new HTTPDataCallback() {
      public int cb (http_parser.lolevel.HTTPParser p, ByteBuffer buf, int pos, int len) {
        byte[] data = fetchBytes(buf, pos, len);
        ((RubyString)requestPath).cat(data);
        return 0;
      }
    };
    this.settings.on_query_string = new HTTPDataCallback() {
      public int cb (http_parser.lolevel.HTTPParser p, ByteBuffer buf, int pos, int len) {
        byte[] data = fetchBytes(buf, pos, len);
        ((RubyString)queryString).cat(data);
        return 0;
      }
    };
    this.settings.on_fragment = new HTTPDataCallback() {
      public int cb (http_parser.lolevel.HTTPParser p, ByteBuffer buf, int pos, int len) {
        byte[] data = fetchBytes(buf, pos, len);
        ((RubyString)fragment).cat(data);
        return 0;
      }
    };

    final RubySymbol arraysSym = runtime.newSymbol("arrays");
    final RubySymbol mixedSym = runtime.newSymbol("mixed");
    final RubySymbol stopSym = runtime.newSymbol("stop");

    this.settings.on_url = new HTTPDataCallback() {
      public int cb (http_parser.lolevel.HTTPParser p, ByteBuffer buf, int pos, int len) {
        checkPause();
        IRubyObject ret = runtime.getNil();
        ThreadContext context = runtime.getCurrentContext();
        byte[] data = fetchBytes(buf, pos, len);

        if (callback_object != null) {
          if (((RubyObject)callback_object).respondsTo("on_url")) {
            ret = callback_object.callMethod(context, "on_url", RubyString.newString(runtime, new ByteList(data, UTF8Encoding.INSTANCE, false)));
          }
        } else if (on_url != null) {
          ret = on_url.call(context, RubyString.newString(runtime, new ByteList(data, UTF8Encoding.INSTANCE, false)));
        }

        if (ret == stopSym) {
          throw new StopException();
        } else {
          ((RubyString)requestUrl).cat(data);
          return 0;
        }
      }
    };

    this.settings.on_status_complete = new HTTPCallback() {
      public int cb (http_parser.lolevel.HTTPParser p) {
        checkPause();
        IRubyObject ret = runtime.getNil();
        ThreadContext context = runtime.getCurrentContext();

        if (callback_object != null) {
          if (((RubyObject)callback_object).respondsTo("on_status_complete")) {
            ret = callback_object.callMethod(context, "on_status_complete");
          }
        } else if (on_status_complete != null) {
          ret = on_status_complete.call(context);
        }

        if (ret == stopSym) {
          throw new StopException();
        } else {
          return 0;
        }
      }
    };

    this.settings.on_header_field = new HTTPDataCallback() {
      public int cb (http_parser.lolevel.HTTPParser p, ByteBuffer buf, int pos, int len) {
        checkPause();
        IRubyObject ret = runtime.getNil();
        ThreadContext context = runtime.getCurrentContext();
        byte[] data = fetchBytes(buf, pos, len);

        if (_current_header == null)
          _current_header = data;
        else {
          byte[] tmp = new byte[_current_header.length + data.length];
          System.arraycopy(_current_header, 0, tmp, 0, _current_header.length);
          System.arraycopy(data, 0, tmp, _current_header.length, data.length);
          _current_header = tmp;
        }

        if (callback_object != null) {
          if (((RubyObject)callback_object).respondsTo("on_header_field")) {
            ret = callback_object.callMethod(context, "on_header_field", RubyString.newString(runtime, new ByteList(data, UTF8Encoding.INSTANCE, false)));
          }
        } else if (on_header_field != null) {
          ret = on_header_field.call(context, RubyString.newString(runtime, new ByteList(data, UTF8Encoding.INSTANCE, false)));
        }

        if (ret == stopSym) {
          throw new StopException();
        } else {
          ((RubyString)requestUrl).cat(data);
          return 0;
        }
       }
    };

    this.settings.on_header_value = new HTTPDataCallback() {
      public int cb (http_parser.lolevel.HTTPParser p, ByteBuffer buf, int pos, int len) {
        checkPause();
        IRubyObject ret = runtime.getNil();
        ThreadContext context = runtime.getCurrentContext();
        byte[] data = fetchBytes(buf, pos, len);
        IRubyObject key, val;
        int new_field = 0;

        if (_current_header != null) {
          new_field = 1;
          _last_header = _current_header;
          _current_header = null;
        }

        key = RubyString.newString(runtime, new ByteList(_last_header, UTF8Encoding.INSTANCE, false));
        val = headers.op_aref(context, key);

        if (new_field == 1) {
          if (val.isNil()) {
            if (header_value_type == arraysSym) {
              headers.op_aset(context, key, RubyArray.newArrayLight(runtime, RubyString.newStringLight(runtime, 10)));
            } else {
              headers.op_aset(context, key, RubyString.newStringLight(runtime, 10));
            }
          } else {
            if (header_value_type == mixedSym) {
              if (val instanceof RubyString) {
                headers.op_aset(context, key, RubyArray.newArrayLight(runtime, val, RubyString.newStringLight(runtime, 10)));
              } else {
                ((RubyArray)val).add(RubyString.newStringLight(runtime, 10));
              }
            } else if (header_value_type == arraysSym) {
              ((RubyArray)val).add(RubyString.newStringLight(runtime, 10));
            } else {
              ((RubyString)val).cat(',').cat(' ');
            }
          }
          val = headers.op_aref(context, key);
        }

        if (val instanceof RubyArray) {
          val = ((RubyArray)val).entry(-1);
        }

        ((RubyString)val).cat(data);

        if (callback_object != null) {
          if (((RubyObject)callback_object).respondsTo("on_header_value")) {
            ret = callback_object.callMethod(context, "on_header_value", RubyString.newString(runtime, new ByteList(data, UTF8Encoding.INSTANCE, false)));
          }
        } else if (on_header_value != null) {
          ret = on_header_value.call(context, RubyString.newString(runtime, new ByteList(data, UTF8Encoding.INSTANCE, false)));
        }

        if (ret == stopSym) {
          throw new StopException();
        } else {
          ((RubyString)requestUrl).cat(data);
          return 0;
        }
      }
    };

    this.settings.on_body = new HTTPDataCallback() {
      public int cb (http_parser.lolevel.HTTPParser p, ByteBuffer buf, int pos, int len) {
        checkPause();
        IRubyObject ret = runtime.getNil();
        ThreadContext context = runtime.getCurrentContext();
        byte[] data = fetchBytes(buf, pos, len);

        if (callback_object != null) {
          if (((RubyObject)callback_object).respondsTo("on_body")) {
            ret = callback_object.callMethod(context, "on_body", RubyString.newString(runtime, new ByteList(data, UTF8Encoding.INSTANCE, false)));
          }
        } else if (on_body != null) {
          ret = on_body.call(context, RubyString.newString(runtime, new ByteList(data, UTF8Encoding.INSTANCE, false)));
        }

        if (ret == stopSym) {
          throw new StopException();
        } else {
          return 0;
        }
      }
    };

    this.settings.on_message_begin = new HTTPCallback() {
      public int cb (http_parser.lolevel.HTTPParser p) {
        checkPause();
        headers = new RubyHash(runtime);

        requestUrl = RubyString.newEmptyString(runtime);
        requestPath = RubyString.newEmptyString(runtime);
        queryString = RubyString.newEmptyString(runtime);
        fragment = RubyString.newEmptyString(runtime);

        upgradeData = RubyString.newEmptyString(runtime);

        IRubyObject ret = runtime.getNil();
        ThreadContext context = runtime.getCurrentContext();

        if (callback_object != null) {
          if (((RubyObject)callback_object).respondsTo("on_message_begin")) {
            ret = callback_object.callMethod(context, "on_message_begin");
          }
        } else if (on_message_begin != null) {
          ret = on_message_begin.call(context);
        }

        if (ret == stopSym) {
          throw new StopException();
        } else {
          return 0;
        }
      }
    };

    this.settings.on_message_complete = new HTTPCallback() {
      public int cb (http_parser.lolevel.HTTPParser p) {
        checkPause();
        IRubyObject ret = runtime.getNil();
        ThreadContext context = runtime.getCurrentContext();

        if (callback_object != null) {
          if (((RubyObject)callback_object).respondsTo("on_message_complete")) {
            ret = callback_object.callMethod(context, "on_message_complete");
          }
        } else if (on_message_complete != null) {
          ret = on_message_complete.call(context);
        }

        if (ret == stopSym) {
          throw new StopException();
        } else {
          return 0;
        }
      }
    };

    this.settings.on_headers_complete = new HTTPCallback() {
      public int cb (http_parser.lolevel.HTTPParser p) {
        checkPause();
        IRubyObject ret = runtime.getNil();
        ThreadContext context = runtime.getCurrentContext();

        if (callback_object != null) {
          if (((RubyObject)callback_object).respondsTo("on_headers_complete")) {
            ret = callback_object.callMethod(context, "on_headers_complete", headers);
          }
        } else if (on_headers_complete != null) {
          ret = on_headers_complete.call(context, headers);
        }

        if (ret == stopSym) {
          throw new StopException();
        } else {
          return 0;
        }
      }
    };
  }

  private void init() {
    this.parser = new HTTPParser();
    this.headers = null;

    this.pause = false;
    this.error = null;

    this.requestUrl = runtime.getNil();
    this.requestPath = runtime.getNil();
    this.queryString = runtime.getNil();
    this.fragment = runtime.getNil();

    this.upgradeData = runtime.getNil();
  }

  @JRubyMethod(name = "initialize")
  public IRubyObject initialize() {
    return this;
  }

  @JRubyMethod(name = "initialize")
  public IRubyObject initialize(IRubyObject arg) {
    callback_object = arg;
    return initialize();
  }

  @JRubyMethod(name = "initialize")
  public IRubyObject initialize(IRubyObject arg, IRubyObject arg2) {
    header_value_type = arg2;
    return initialize(arg);
  }

  @JRubyMethod(name = "on_url")
  public void set_on_url(Block block) {
    on_url = block;
  }

  @JRubyMethod(name = "on_status_complete")
  public void set_on_status_complete(Block block) {
    on_status_complete = block;
  }

  @JRubyMethod(name = "on_header_field")
  public void set_on_header_field(Block block) {
    on_header_field = block;
  }

  @JRubyMethod(name = "on_header_value")
  public void set_on_header_value(Block block) {
    on_header_value = block;
  }

  @JRubyMethod(name = "on_body")
  public void set_on_body(Block block) {
    on_body = block;
  }

  @JRubyMethod(name = "on_message_begin")
  public void set_on_message_begin(Block block) {
    on_message_begin = block;
  }

  @JRubyMethod(name = "on_message_complete")
  public void set_on_message_complete(Block block) {
    on_message_complete = block;
  }

  @JRubyMethod(name = "on_headers_complete")
  public void set_on_headers_complete(Block block) {
    on_headers_complete = block;
  }

  @JRubyMethod(name = "<<")
  public IRubyObject execute(IRubyObject data) {
    RubyString str = (RubyString)data;
    ByteList byteList = str.getByteList();
    ByteBuffer buf = ByteBuffer.wrap(byteList.getUnsafeBytes(), byteList.getBegin(), byteList.getRealSize());
    boolean stopped = false;

    try {
      this.parser.execute(this.settings, buf);
    } catch (HTTPException e) {
      this.error = e.getMessage();
      throw new RaiseException(runtime, eParserError, e.getMessage(), true);
    } catch (StopException e) {
      stopped = true;
    }

    if (parser.getUpgrade()) {
      byte[] upData = fetchBytes(buf, buf.position(), buf.limit() - buf.position());
      ((RubyString)upgradeData).cat(upData);

    } else if (buf.hasRemaining()) {
      if (!stopped)
        throw new RaiseException(runtime, eParserError, "Could not parse data entirely", true);
    }

    return RubyNumeric.int2fix(runtime, buf.position());
  }

  @JRubyMethod(name = "keep_alive?")
  public IRubyObject shouldKeepAlive() {
    return runtime.newBoolean(parser.shouldKeepAlive());
  }

  @JRubyMethod(name = "upgrade?")
  public IRubyObject shouldUpgrade() {
    return runtime.newBoolean(parser.getUpgrade());
  }

  @JRubyMethod(name = "http_major")
  public IRubyObject httpMajor() {
    if (parser.getMajor() == 0 && parser.getMinor() == 0)
      return runtime.getNil();
    else
      return RubyNumeric.int2fix(runtime, parser.getMajor());
  }

  @JRubyMethod(name = "http_method")
  public IRubyObject httpMethod() {
    HTTPMethod method = parser.getHTTPMethod();
    if (method != null)
      return runtime.newString(new String(method.bytes));
    else
      return runtime.getNil();
  }

  @JRubyMethod(name = "http_minor")
  public IRubyObject httpMinor() {
    if (parser.getMajor() == 0 && parser.getMinor() == 0)
      return runtime.getNil();
    else
      return RubyNumeric.int2fix(runtime, parser.getMinor());
  }

  @JRubyMethod(name = "http_version")
  public IRubyObject httpVersion() {
    if (parser.getMajor() == 0 && parser.getMinor() == 0)
      return runtime.getNil();
    else
      return runtime.newString(httpMajor() + "." + httpMinor());
  }

  @JRubyMethod(name = "http_status")
  public IRubyObject httpStatus() {
    int code = parser.getStatusCode();
    if (code != 0)
      return RubyNumeric.int2fix(runtime, code);
    else
      return runtime.getNil();
  }

  @JRubyMethod(name = "headers")
  public IRubyObject getHeaders() {
    return headers == null ? runtime.getNil() : headers;
  }

  @JRubyMethod(name = "request_url")
  public IRubyObject getRequestUrl() {
    return requestUrl == null ? runtime.getNil() : requestUrl;
  }

  @JRubyMethod(name = "request_path")
  public IRubyObject getRequestPath() {
    return requestPath == null ? runtime.getNil() : requestPath;
  }

  @JRubyMethod(name = "query_string")
  public IRubyObject getQueryString() {
    return queryString == null ? runtime.getNil() : queryString;
  }

  @JRubyMethod(name = "fragment")
  public IRubyObject getFragment() {
    return fragment == null ? runtime.getNil() : fragment;
  }

  @JRubyMethod(name = "header_value_type")
  public IRubyObject getHeaderValueType() {
    return header_value_type == null ? runtime.getNil() : header_value_type;
  }

  @JRubyMethod(name = "header_value_type=")
  public IRubyObject set_header_value_type(IRubyObject val) {
    String valString = val.toString();
    if (valString != "mixed" && valString != "arrays" && valString != "strings") {
      throw runtime.newArgumentError("Invalid header value type");
    }
    header_value_type = val;
    return val;
  }

  @JRubyMethod(name = "upgrade_data")
  public IRubyObject upgradeData() {
    return upgradeData == null ? runtime.getNil() : upgradeData;
  }

  @JRubyMethod(name = "reset!")
  public IRubyObject reset(IRubyObject type) {
    init();
    return runtime.getTrue();
  }

  @JRubyMethod(name = "error")
  public IRubyObject error() {
    if (this.error == null) {
      return runtime.getNil();
    } else {
      return runtime.newString(this.error);
    }
  }

  @JRubyMethod(name = "error?")
  public IRubyObject isError() {
    if (this.error == null) {
      return runtime.getFalse();
    } else {
      return runtime.getTrue();
    }
  }

  @JRubyMethod(name = "pause")
  public void pause() {
    this.pause = true;
  }

  @JRubyMethod(name = "resume")
  public void resume() {
    this.pause = false;
  }

  @JRubyMethod(name = "paused?")
  public IRubyObject isPaused() {
    return runtime.newBoolean(this.pause);
  }

  public void checkPause() {
    if (this.pause) {
      throw new RaiseException(runtime, eParserError, "http parser pause", true);
    }
  }
}
