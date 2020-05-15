/*
  Copyright © 2018 Booz Allen Hamilton. All Rights Reserved.
  This software package is licensed under the Booz Allen Public License. The license can be found in the License file or at http://boozallen.github.io/licenses/bapl
*/

import hudson.AbortException

@Validate // validate so this runs prior to other @Init steps
void call(context){
    node{
        cleanWs()
        println "about to checkout SCM"
        try{
            checkout scm
            println "checked out SCM"
        }catch(AbortException ex) {
            println "scm var not present, skipping source code checkout" 
        }
        println "about to create workspace stash"
        stash name: 'workspace', allowEmpty: true, useDefaultExcludes: false
        println "created workspace stash"
    }
}