package scipost

class Util {
    // Create a activity id from a URL
    static def makeActivityId(url,id) {
        if (url.startsWith('https://arxiv')) {
            return url + "/event/" + id 
        }
        else {
            return url + "event/" + id 
        }
    }

    // Create a URL from a sciPost preprint url
    static def makePreprintUrl(url) {
        if (url.startsWith('/')) {
            return 'https://scipost.org' + url
        }
        else {
            return url
        }
    }

    // Make a DOI from a preprint URL 
    static def makeDOI(url) {
        if (url.startsWith('https://arxiv')) {
            return 'https://doi.org/10.48550/' + 
                    url.replaceAll('.*/','')
        }
        else {
            return 'https://doi.org/10.21468/' +
                    url.replaceAll('/$','').replaceAll('.*/','')
        }
    }

    // Parse the SciPost authors
    static def parseAuthor(str) {
        def parts = str.split(/\s*,\s*/)
        return parts
    }

    // Mock webid lookup for an author
    static def webidLookup(name) {
        def webidName = name.replaceAll(' ','_').toLowerCase()

        return "https://${webidName}.arxiv.org/profile/card#me"
    }

    // Mock origin lookup for an author
    static def originLookup(name) {
        return [
            'id'   : 'https://arxiv.org/profile/card$#me' ,
            'name' : 'arXiv pod' ,
            'type' : 'Application'
        ]
    }

    // Mock target lookup for this service
    static def targetLookup() {
        return [
            'id'   : 'https://scipost.org/profile/card#me' ,
            'name' : 'Scipost service' ,
            'type' : 'Application'
        ]
    }
}