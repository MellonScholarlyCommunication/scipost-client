package scipost

class Util {
    // Create a activity id from a URL
    // All activity identifiers will be events (part of an event logs)
    static def makeActivityId(url,id) {
        def frag = url.replaceAll("v[0-9]+/?\$","")
        return frag + "/event/" + id 
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
    static def webidLookup(name,context) {
        def webidName = name.replaceAll(' ','_').toLowerCase()

        def iriPart = context.replaceAll('https://','')
                             .replaceAll('/.*','')

        return "https://${iriPart}/profile/${webidName}/card#me"
    }

    // Mock inbox lookup for an author
    static def inboxLookup(name,context) {
        def webidName = name.replaceAll(' ','_').toLowerCase()

        def iriPart = context.replaceAll('https://','')
                             .replaceAll('/.*','')

        return "https://${iriPart}/inbox/${webidName}/"
    } 

    // Mock origin lookup for an author
    static def originLookup(name,context) {
        def iriPart = context.replaceAll('https://','')
                             .replaceAll('/.*','')

        def serviceName = ''

        if (iriPart.matches(".*arxiv.*")) {
            serviceName = 'arXiv repository'
        }
        else {
            serviceName = 'Scipost repository'
        }

        return [
            'id'    : "https://${iriPart}/profile/card#me" ,
            'name'  : serviceName ,
            'type'  : 'Application'
        ]
    }
}