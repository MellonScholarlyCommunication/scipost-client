#!/bin/bash

./bin/scipost_client.groovy submissions  > data/submissions.json
./bin/scipost_client.groovy publications > data/publications.json