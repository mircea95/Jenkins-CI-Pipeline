package com.internship.tls

class Shared implements Serializable {
    def pipelineContext

    Shared(pipelineContext) {
        this.pipelineContext = pipelineContext
    }

    def getVersion(String fileName){
        def fileType = fileName.split(/\./)[-1]

        if (fileType == "xml") {
            return pipelineContext.readMavenPom().getVersion().split("-")[0]
        }

        if (fileType == "json") {
            return pipelineContext.readJSON(file: 'package.json').version
        }
    }

    def gitPathUrl(String gitUrl){
        def processUrl = gitUrl.split(/\/|\./);
        processUrl = (processUrl as List)[6..-2] as String[]

        return processUrl.join("%2F")
    }

    def getVersionFromMain(String tagsFile){
        def lastTag = pipelineContext.readJSON(file: tagsFile)[0].name.split('\\.')
        lastTag = (lastTag as List)[0..-2] as String[]
        
        return lastTag.join("") as int
    }
}