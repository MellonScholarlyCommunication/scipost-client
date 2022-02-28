package scipost 

import groovy.json.*

class SciPostClient {
    def cache_loop(file,closure) {
        def jsonSlurper = new JsonSlurper()

        new File(file).withReader('UTF-8') {
            reader -> {
                def line
                while( (line = reader.readLine()) != null) {
                    if (closure) 
                        closure.call(jsonSlurper.parseText(line))
                }
            }
        }
    }

    def api_loop(base,type,closure,options) {
        def next = "${base}/${type}/"
        def jsonSlurper = new JsonSlurper()

        do {
            try {
                System.err.println("Downloading ${next} ...")

                def connection = new URL(next).openConnection()
                connection.setRequestProperty('Accept','application/json')

                def response = connection.inputStream.text

                def json = jsonSlurper.parseText(response)

                def count   = json['count']
                def results = json['results']

                for (res in results) {
                    if (closure)
                        closure.call(res)
                }

                next = json['next']

                if (options && options['sleep']) {
                    sleep( options['sleep'] * 1000)
                }
            }
            catch (e) {
                System.err.println("whoops ${e}")
            }
        } while (next && next.length() > 0)
    }
}