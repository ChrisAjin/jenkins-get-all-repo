import jenkins.model.*
import hudson.model.*
import hudson.plugins.git.*
import org.jenkinsci.plugins.workflow.job.*
import org.jenkinsci.plugins.workflow.cps.*
import org.jenkinsci.plugins.workflow.multibranch.*

def view = Jenkins.instance.getView("All") // view
def allGitUrls = [] as Set

view.items.each { job ->
    try {
        def jobName = job.fullName

        if (job instanceof FreeStyleProject) {
            def scm = job.getScm()
            if (scm instanceof GitSCM) {
                scm.getUserRemoteConfigs().each { config ->
                    def url = config.getUrl()
                    println("${jobName},${url}")
                    allGitUrls << url
                }
            }
        }

        else if (job instanceof WorkflowJob) {
            def defn = job.getDefinition()
            if (defn instanceof CpsFlowDefinition) {
                def scriptText = defn.getScript()
                def matcher = scriptText =~ /url\s*:\s*['"]([^'"]+)['"]/
                matcher.each { match ->
                    def url = match[1]
                    println("${jobName},${url}")
                    allGitUrls << url
                }
            } else if (defn instanceof CpsScmFlowDefinition) {
                // Jenkinsfile repo
                def scm = defn.getScm()
                if (scm instanceof GitSCM) {
                    scm.getUserRemoteConfigs().each { config ->
                        def url = config.getUrl()
                        println("${jobName},${url}")
                        allGitUrls << url
                    }
                }
            }
        }

        // Multibranch pipelines
        else if (job instanceof WorkflowMultiBranchProject) {
            job.getSCMSources().each { source ->
                try {
                    def remote = source?.remote
                    if (remote?.contains("git")) {
                        println("${jobName},${remote}")
                        allGitUrls << remote
                    }
                } catch (MissingPropertyException ignored) {}
            }
        }

    } catch (Exception e) {
        println("Error for ${job.fullName}: ${e.message}")
    }
}

println " ${allGitUrls.size()} URLs:"
allGitUrls.each { println it }
