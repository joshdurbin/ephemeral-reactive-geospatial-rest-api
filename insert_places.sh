#!/usr/bin/env bash

# use one worker with one thread and limit to 90 seconds (should be plenty of time to finish)
wrk -c 1 -t 1 -d 90s -s insert_places.lua http://localhost:5050
