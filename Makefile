.PHONY: help data events clean

help:
	@echo "usage: make [target]"

data:
	./bin/download.sh

events:
	./bin/scipost2events.groovy

clean:
	rm -rf output/* 
