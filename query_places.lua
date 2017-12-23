json = require "json"

function file_exists(file)
  local f = io.open(file, "rb")
  if f then f:close() end
  return f ~= nil
end

function lines_from(file)
  if not file_exists(file) then return {} end
  lines = {}
  for line in io.lines(file) do 
    lines[#lines + 1] = line
  end
  return lines
end

placesPerLine = lines_from("bayareaplaces.json")

if #placesPerLine <= 0 then
  print("seed_places: no places found")
  os.exit()
end

counter = 1

request = function()

  local place = placesPerLine[counter]
  counter = counter + 1

  if counter > #placesPerLine then
    os.exit()
  end

  wrk.method = "GET"

  local placeData = json.decode(place)

  return wrk.format(nil, "/api/v0/places/near/" .. placeData.latitude .. "/" .. placeData.longitude)
end
