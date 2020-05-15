/*
  Copyright © 2018 Booz Allen Hamilton. All Rights Reserved.
  This software package is licensed under the Booz Allen Public License. The license can be found in the License file or at http://boozallen.github.io/licenses/bapl
*/

import org.boozallen.plugins.jte.init.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.init.dsl.PipelineConfigurationDsl

@Init
void call(context){
    PipelineConfigurationObject aggregated = new PipelineConfigurationObject(
        config: pipelineConfig, // variable provided in binding by JTE
        merge: [],
        override: []
    )
    node{
        writeFile text: PipelineConfigurationDsl.serialize(aggregated), file: "pipeline_config.groovy"
        archiveArtifacts "pipeline_config.groovy"
    }
}