#!/usr/bin/env groovy -cp lib

import scipost.SciPostClient
import scipost.Util
import groovy.json.JsonOutput

def submissionsFile = './data/submissions.json'
def publicatonsFile = './data/publications.json'

def submissionCollector = [:]

new SciPostClient().cache_loop(submissionsFile, {
    x -> submissionCollect(x,submissionCollector)
})

submissionCollector.each { entry -> 
    preprintOfferProcessor(entry.value) 
    acceptOfferProcessor(entry.value)
}

def submissionCollect(submission, collector) {
    def thread_hash     = submission['thread_hash']

    if (! collector[thread_hash]) {
        collector[thread_hash] = []
    }

    collector[thread_hash].push(submission)
}

// Preprint Offer processor takes all submissions 
// and treat them as new offers to the SciPost service
// Offers can be of two types:
//    - Arxiv -> SciPost
//    - SciPost preprint -> SciPost
def preprintOfferProcessor(thread) {
    for (submission in thread) {
        def preprint   = submission['preprint']['url'].replaceAll("http:","https:")
        def thread_id  = submission['thread_hash']
        def thread_seq = submission['thread_sequence_order']

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
            'actor' : [
                'id'    : actor_id ,
                'name'  : authors[0] ,
                'inbox' : actor_inbox ,
                'type'  : 'Person'
            ] ,
            'object' : [
                'id' : object_id ,
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

        // Create a file name out of the activity identifier
        def outputFile = "output/" + makeActivityFile(id)

        System.err.println("Writing to ${outputFile}")
        
        createActivityDir(outputFile)

        new File(outputFile).write(
           JsonOutput.prettyPrint(json) 
        )
    }
}

// Accept Offer takes all submissons and act as if
// for each of them an Accept was sent back to the author
def acceptOfferProcessor(thread) {
    for (submission in thread) {
        def service    = "https://scipost.org/submissons"

        def preprint   = submission['preprint']['url'].replaceAll("http:","https:")
        def thread_id  = submission['thread_hash']
        def thread_seq = submission['thread_sequence_order']

        // The activity identifier is the service URL plus thread identifiers
        def id         = Util.makeActivityId(service,"02.${thread_id}.${thread_seq}.accept")

        def object_id    = Util.makePreprintUrl(preprint)
        def inReplyTo    = Util.makeActivityId(object_id,"01.${thread_id}.${thread_seq}.offer")
        def offer_id     = Util.makeActivityId(object_id,"01.${thread_id}.${thread_seq}.offer")
        def authors      = Util.parseAuthor(submission['author_list'])
        def target_id    = Util.webidLookup(authors[0],object_id)
        def target_inbox = Util.inboxLookup(authors[0],object_id)
        def cite_as      = Util.makeDOI(object_id)

        def event = [
            '@context' : 'https://www.w3.org/ns/activitystreams' ,
            'id' : id ,
            'type' : 'Accept' ,
            'actor' : [
                'id'    : 'https://scipost.org/profile/card#me' ,
                'name'  : 'Scipost service' ,
                'inbox' : 'https://scipost.org/inbox/' ,
                'type'  : 'Application'
            ],
            'context' : [
                'id' : object_id ,
                'ietf:cite-as': cite_as ,
                'type' : 'Document'
            ] ,
            'inReplyTo': inReplyTo,
            'object' : [
                'id' : inReplyTo ,
                'object' : object_id ,
                'type': 'Offer'
            ] ,
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

        // Create a file name out of the activity identifier
        def outputFile = "output/" + makeActivityFile(id)

        System.err.println("Writing to ${outputFile}")
        
        createActivityDir(outputFile)

        new File(outputFile).write(
           JsonOutput.prettyPrint(json) 
        )
        
        // Create the same event for the preprint

        def object_event = Util.makeActivityId(object_id,"02.${thread_id}.${thread_seq}.accept")
        outputFile = "output/" + makeActivityFile(object_event)

        System.err.println("Writing to ${outputFile}")
        
        createActivityDir(outputFile)

        new File(outputFile).write(
           JsonOutput.prettyPrint(json) 
        ) 
    }
}

// Announce Publication takes all publications and act as if
// for each of them an Announce was sent back to the author
def announcePublicationProcessor(thread) {
}

def makeActivityFile(id) {
    return id.replaceAll("https://","")
}

def createActivityDir(id) {
    def parentDir = new File(id).getParent()
    if (! new File(parentDir).exists() )
        new File(parentDir).mkdirs() 
}