local random = math.random

request = function()

  wrk.method = "GET"

  local latWhole = random(37, 42)
  local latDec = random(random(0,999999))

  local longWhole = random(118, 123)
  local longDec = random(random(0,999999))

  local resource = "/api/v0/places/near/" .. latWhole .. "." .. latDec .. "/-" .. longWhole .. "." .. longDec .. "/50"
   
  return wrk.format(nil, resource)
end
