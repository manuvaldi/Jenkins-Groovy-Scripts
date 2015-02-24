// Automatic Cleaning of Workspaces in Slaves.
// If freespace of the each slave is less than "mindisk" var, automatically the agent change to offline and job's workspaces are cleaned.

import hudson.model.*;
import hudson.util.*;
import jenkins.model.*;
import hudson.FilePath.FileCallable;
import hudson.slaves.OfflineCause;
import hudson.node_monitors.*;

// Disk space minumum in GB
mindisk=20
// For testing purpose
dryrun="no"

for (node in Jenkins.instance.nodes) {
    computer = node.toComputer()

    // Filter slaves by name
    //if ( ! node.name.contains("Dynamic Slave")) continue


    println("SLAVE: " + node.name)

    if (computer.getChannel() == null) continue

    rootPath = node.getRootPath()
    size = DiskSpaceMonitor.DESCRIPTOR.get(computer).size
    roundedSize = size / (1024 * 1024 * 1024) as int
    println(" * Free Space: " + roundedSize + "GB")

    if (roundedSize < mindisk) {
        if (dryrun == "no") {      
            computer.setTemporarilyOffline(true, new hudson.slaves.OfflineCause.ByCLI("Disk Cleaning Task"))
        }
        println(" * Cleaning job's workspaces.... (" + roundedSize + "GB < " + mindisk + "GB )")
        for (item in Jenkins.instance.items) {
            jobName = item.getFullDisplayName()

            if (item.isBuilding()) {
               println("   - JOB - " + jobName)
               println("       WARNING - " + jobName + " is currently running, skipped")
               continue
            }

           workspacePath = node.getWorkspaceFor(item)
           if (workspacePath == null) {
            continue
           }

            if (item.getCustomWorkspace() != null) {
               customWorkspace = item.getCustomWorkspace()
               workspacePath = node.getRootPath().child(customWorkspace)
            }

            pathAsString = workspacePath.getRemote()
            if (workspacePath.exists()) {
                println("   - JOB - " + jobName)
                if (dryrun == "no") {
                    workspacePath.deleteRecursive()
                    println("       Location deleted: " + pathAsString)
                }
            }
        }
        if (dryrun == "no") {
            computer.setTemporarilyOffline(false, null)
        }
    }
}


