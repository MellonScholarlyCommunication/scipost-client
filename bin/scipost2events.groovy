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
        def id         = Util.makeActivityId(object_id,"${thread_id}.${thread_seq}")
        def authors    = Util.parseAuthor(submission['author_list'])
        def actor_id   = Util.webidLookup(authors[0])
        def origin     = Util.originLookup(authors[0])
        def target     = Util.targetLookup()
        def cite_as    = Util.makeDOI(object_id)

        def event = [
            '@context' : 'https://www.w3.org/ns/activitystreams' ,
            'id' : id ,
            'type' : 'Offer' ,
            'actor' : [
                'id'   : actor_id ,
                'name' : authors[0] ,
                'type' : 'Person'
            ] ,
            'object' : [
                'id' : object_id ,
                'ietf:cite-as': cite_as ,
                'type' : 'Document'
            ] ,
            'origin' : origin ,
            'target' : target
        ]

        def json = JsonOutput.toJson(event)

        def outputFile = "output/" + makeEventFile(id)

        System.err.println("Writing to ${outputFile}")
        
        createEventDir(outputFile)

        new File(outputFile).write(
           JsonOutput.prettyPrint(json) 
        )
    }
}

// Accept Offer takes all submissons and act as if
// for each of them an Accept was send back to the author
def acceptOfferProcessor(thread) {
    for (submission in thread) {
        def service    = "https://scipost.org/submissons"

        def preprint   = submission['preprint']['url'].replaceAll("http:","https:")
        def thread_id  = submission['thread_hash']
        def thread_seq = submission['thread_sequence_order']

        def id         = Util.makeActivityId(service,"${thread_id}.${thread_seq}")
        def object_id  = Util.makePreprintUrl(preprint)
        def inReplyTo  = Util.makeActivityId(object_id,"${thread_id}.${thread_seq}")
        def offer_id   = Util.makeActivityId(object_id,"${thread_id}.${thread_seq}")
        def authors    = Util.parseAuthor(submission['author_list'])
        def target_id  = Util.webidLookup(authors[0])
        def cite_as    = Util.makeDOI(object_id)

        def event = [
            '@context' : 'https://www.w3.org/ns/activitystreams' ,
            'id' : id ,
            'type' : 'Accept' ,
            'actor' : [
                'id'   : 'https://scipost.org/person/admin/profile/card#me' ,
                'name' : 'Scipost autoreply robot' ,
                'type' : 'Software'
            ] ,
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
                'id'   : target_id ,
                'name' : authors[0] ,
                'type' : 'Person' 
            ]
        ]

        def json = JsonOutput.toJson(event)

        def outputFile = "output/" + makeEventFile(id)

        System.err.println("Writing to ${outputFile}")
        
        createEventDir(outputFile)

        new File(outputFile).write(
           JsonOutput.prettyPrint(json) 
        )
    }
}

def makeEventFile(id) {
    return id.replaceAll("https://","")
}

def createEventDir(id) {
    def parentDir = new File(id).getParent()
    if (! new File(parentDir).exists() )
        new File(parentDir).mkdirs() 
}