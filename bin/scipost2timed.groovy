#!/usr/bin/env groovy -cp lib
/**
 * Only output the real events that happened at SciPost without
 * Mellon generated events
 */
import scipost.SciPostClient
import scipost.Util
import groovy.json.JsonOutput
import groovy.transform.Field

def submissionsFile = './data/submissions.json'
def publicatonsFile = './data/publications.json'

@Field submissionCollector = [:]

new SciPostClient().cache_loop(submissionsFile, {
    x -> Util.threadCollector(x,submissionCollector)
})

submissionCollector.each { entry -> 
    submission2timedEvent(entry.value)
}

new SciPostClient().cache_loop(publicatonsFile, {
    x -> publication2timedEvent(x)
})

def submissionLookup(publication) {
    def accepted_submission = publication['accepted_submission'] 

    for (entry in submissionCollector) {
        if (entry.value[-1]['url'].equals(accepted_submission)) {
            return entry.value[-1]
        }   
    }

    return null
}

def submission2timedEvent(thread) {
     for (submission in thread) {
        def preprint   = submission['preprint']['url'].replaceAll("http:","https:")
        def thread_id  = submission['thread_hash']
        def thread_seq = submission['thread_sequence_order']
        def title      = submission['title']
        def published  = submission['submission_date'] + "T00:00:00Z"

        def object_id  = Util.makePreprintUrl(preprint)

        // The activity identifier is the preprint URL plus thread identifiers
        // We mimic the behaviour of a repository 
        def id          = Util.makeActivityId(object_id,"01.${thread_id}.${thread_seq}.offer")
        def authors     = Util.parseAuthor(submission['author_list'])
        def actor_id    = Util.webidLookup(authors[0],object_id)
        def actor_inbox = Util.inboxLookup(authors[0],object_id)
        def origin      = Util.originLookup(authors[0],object_id)
        def cite_as     = Util.makeDOI(object_id)

        def event = [
            '@context' : 'https://www.w3.org/ns/activitystreams' ,
            'id' : id ,
            'type' : 'Offer' ,
            'published' : published ,
            'actor' : [
                'id'    : actor_id ,
                'name'  : authors[0] ,
                'inbox' : actor_inbox ,
                'type'  : 'Person'
            ] ,
            'object' : [
                'id' : object_id ,
                'title' : title ,
                'ietf:cite-as': cite_as ,
                'type' : 'Document'
            ] ,
            'origin' : origin ,
            'target' : [
                'id'    : 'https://scipost.org/profile/card#me' ,
                'name'  : 'Scipost service' ,
                'inbox' : 'https://scipost.org/inbox/' ,
                'type'  : 'Application'
            ]
        ]

        def json = JsonOutput.toJson(event)

        println(json)
     }
}

def publication2timedEvent(publication) {
    def submission = submissionLookup(publication)

    if (submission == null) {
        System.err.println("warning no submission for ${publication['url']}")
        return
    }

    def preprint     = submission['preprint']['url'].replaceAll("http:","https:")
    def thread_id    = submission['thread_hash']
    def thread_seq   = submission['thread_sequence_order']
    def stitle       = submission['title'] 
    def title        = publication['title']

    def service      = "https://scipost.org/submissions/${thread_id}.${thread_seq}"
    def id           = Util.makeActivityId(service,"03.${thread_id}.${thread_seq}.announce")


    def object_id    = Util.makePreprintUrl(preprint)
    def inReplyTo    = Util.makeActivityId(object_id,"01.${thread_id}.${thread_seq}.offer")

    def authors      = Util.parseAuthor(submission['author_list'])
    def target_id    = Util.webidLookup(authors[0],object_id)
    def target_inbox = Util.inboxLookup(authors[0],object_id)

    def scite_as     = Util.makeDOI(object_id)
    def cite_as      = 'https://doi.org/' + publication['doi']
    def publication_id = 'https://scipost.org' + publication['url'] 

    def published    = publication['publication_date'] + 'T00:00:00Z'

    def event = [
        '@context' : 'https://www.w3.org/ns/activitystreams' ,
        'id'   : "${thread_id}.${thread_seq}.announce" ,
        'type' : 'Announce' ,
        'published' : published ,
        'actor' : [
            'id'    : 'https://scipost.org/profile/card#me' ,
            'name'  : 'Scipost service' ,
            'inbox' : 'https://scipost.org/inbox/' ,
            'type'  : 'Application'
        ],
        'object' : [
            'id' : publication_id ,
            'ietf:cite-as' : scite_as ,
            'title' : title ,
            'type' : 'Page'
        ] ,
        'inReplyTo': inReplyTo ,
        'context' : [
            'id'    : object_id ,
            'title' : stitle,
            'ietf:cite-as': scite_as,
            'type'  : 'Document'
        ],
        'origin' : [
            'id' : 'https://scipost.org/profile/card#me',
            'name': 'Scipost service',
            'type': 'Application'
        ] ,
        'target' : [
            'id'    : target_id ,
            'name'  : authors[0] ,
            'inbox' : target_inbox ,
            'type'  : 'Person' 
         ]
    ]

    def json = JsonOutput.toJson(event)

    println(json)
}