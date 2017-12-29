local random = math.random

request = function()

  local latWhole = random(37, 42)
  local latDec = random(random(0,999999))

  local longWhole = random(118, 123)
  local longDec = random(random(0,999999))

  wrk.method = "POST"
  wrk.headers["Content-Type"] = "application/json"
  wrk.body = '{"address":"1646 N California Blvd","longitude":-' .. longWhole .. '.' .. longDec .. ',"zipCode":"94596","state":"CA","latitude":' .. latWhole .. '.' .. latDec .. ',"telephoneNumber":"(925) 943-1775","name":"Diablo Ballet","city":"Walnut Creek"}'

  return wrk.format(nil, "/api/v0/places")
end
