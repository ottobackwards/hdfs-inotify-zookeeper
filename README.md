## hdfs-inotify-zookeeper

The goal of this library is to allow systems that recieve change notifications from zookeeper 
to track changes triggered by hdfs fs change events.

For example, a storm topology loads some configuration from hdfs, and can reload the configuration
if it can tell it changed.

Because the notification from INotify HDFS cannot be filtered at this time, you do not want to 
do this on all events in every worker.

If the system already supports monitoring other configurations in zookeeper ( like apache metron does),
then it would be enough to be able to flag changes in zookeeper and have the system
then reload from hdfs.

To do this, what I am going to do is:

- create an area in zookeeper for the registration of hdfs paths to watch and 'change' nodes to
be watched by the system

- create an inotify client, that reads these registrations, and uses the paths as match rules

- this client, when it matches an event will then 'notify' zookeeper by affecting a change
in the change node


The idea of this POC is to see if this is feasible for the apache metron project
