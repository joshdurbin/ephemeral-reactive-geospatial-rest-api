#!/usr/bin/env bash

# use one worker with one thread and limit to 90 seconds (should be plenty of time to finish)
wrk -c 250 -t 45 -d 30s -s insert_random_places.lua http://localhost:5050
