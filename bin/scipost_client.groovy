#!/usr/bin/env groovy -cp ./lib

import groovy.json.*
import scipost.SciPostClient

def usage() {
    System.err.println("""
usage: scipost_client.groovy publications|submissions
""")
    System.exit(1)
}

def sleep    = 0
def BASE_URL = 'https://scipost.org/api'
def cli      = new CliBuilder()

cli.with {
    s(longOpt: 'sleep', 'Sleep', args: 1, required: false)
}

def options = cli.parse(args)

if (options && options.s) {
    sleep = parseInt(options.s)
}

if (options.arguments().size() != 1) {
    usage()
}

def type = options.arguments()[0]

new SciPostClient().api_loop(BASE_URL, type, {
    x -> println(JsonOutput.toJson(x)) 
} , [ 'sleep' : sleep ])