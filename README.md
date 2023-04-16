# validator-demo

This is a Java/Kotlin code base that show case how you can use the openapi4j library to valdiate an incoming HTTP request's request body against an OAS 3.0 spec

Furthermore, once this code has been turned into a "server" listening on say localhost:8080, we can point a Kong API gateway to send traffic to it via a Kong plugin

# ChatGPT Q&A to create a Kong plugin for this

## Question

Can you write a Kong plugin that calls another server at `POST http://localhost:8080/request-validations`. Here are the requirements:
- Please pass the originally requested URL (including parameters and fragments) as the header `X-Forwarded-URL`
- Please pass the originally requested headers
- Please pass the originally requested body
- Please pass the original method as `X-Forwarded-Method`
- Ensure if the server call to localhost:8080 returns 200, then let the request to downstream, if not then send back the full response (header and body) to the caller
- Make sure you do not exhaust the incoming body, that is when we receive the "200" we still need to pass the body to the upstream so you cannot read the body once and discard it

## ChatGPT Answer

You can create a custom Kong plugin by following these steps:

Create a directory for your plugin, for example: my_request_validation_plugin
Inside this directory, create a kong/plugins/my_request_validation_plugin directory structure
Inside the my_request_validation_plugin directory, create a handler.lua file and a schema.lua file
Here's the content of the handler.lua file:

```lua
local BasePlugin = require "kong.plugins.base_plugin"
local http = require "resty.http"
local cjson = require "cjson"

local MyRequestValidationHandler = BasePlugin:extend()

function MyRequestValidationHandler:new()
  MyRequestValidationHandler.super.new(self, "my_request_validation_plugin")
end

function MyRequestValidationHandler:access(conf)
  MyRequestValidationHandler.super.access(self)

  local forwarded_url = ngx.var.request_uri
  local forwarded_method = ngx.req.get_method()

  local client = http.new()

  local original_headers = ngx.req.get_headers()
  original_headers["X-Forwarded-URL"] = forwarded_url
  original_headers["X-Forwarded-Method"] = forwarded_method

  ngx.req.read_body()
  local original_body = ngx.req.get_body_data()

  local res, err = client:request_uri("http://localhost:8080/request-validations", {
    method = "POST",
    headers = original_headers,
    body = original_body,
  })

  if not res then
    ngx.log(ngx.ERR, "Failed to make request to request-validations: ", err)
    return ngx.exit(500)
  end

  if res.status ~= 200 then
    ngx.status = res.status
    for k, v in pairs(res.headers) do
      ngx.header[k] = v
    end
    ngx.say(res.body)
    return ngx.exit(res.status)
  end
end

return MyRequestValidationHandler
```

And here's the content of the schema.lua file:

```lua
local typedefs = require "kong.db.schema.typedefs"

return {
  name = "my_request_validation_plugin",
  fields = {
    { consumer = typedefs.no_consumer },
    { config = {
        type = "record",
        fields = {},
      },
    },
  },
}
```

Finally, you can add your plugin to Kong's configuration by adding it to the plugins list in the kong.conf file:

```
plugins = bundled,my_request_validation_plugin
```

And load your custom plugin using the KONG_PLUGINS environment variable:

```
export KONG_PLUGINS=bundled,my_request_validation_plugin
```

Then, start or restart Kong for the changes to take effect.

This plugin will intercept the incoming request, forward it to http://localhost:8080/request-validations with the required headers, and handle the response according to your requirements.
