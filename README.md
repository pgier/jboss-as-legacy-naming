jboss-as-legacy-naming
======================

Legacy Naming extension

The full documentation is at https://mojo.redhat.com/docs/DOC-938349

EAP5 used a different naming and remote protocols than EAP6. 
Thus when you want to interact with EAP6 from an EAP5 client (or server) you are confronted with a lot of issues.
One solution would be to upgrade your client to EAP6, but sometimes this is not as simple as it seems.
For these cases we have developed a set of extensions for seamless integration of legacy clients with EAP6 and above.


Full configuration example:

    <extensions>  
    ...  
      <extension module="org.jboss.legacy.jnp"/>  
    </extensions>  
    ...  
    <subsystem xmlns="urn:jboss:doma:in:legacy-jnp:1.0">  
      <jnp-server/>  
      <jnp-connector socket-binding="jnp" rmi-socket-binding="rmi-jnp" />  
      <remoting socket-binding="legacy-remoting"/>  
      <distributed-cache cache-container="singleton" cache-ref="default" />  
    </subsystem>  
    <subsystem xmlns="urn:jboss:domain:infinispan:1.4">  
      <cache-container name="singleton" aliases="cluster ha-partition" default-cache="default">  
        <transport lock-timeout="60000"/>  
        <replicated-cache name="default" mode="SYNC" batching="true">  
          <locking isolation="REPEATABLE_READ"/>  
        </replicated-cache>  
      </cache-container>  
      ...  
    </subsystem>  
    ...  
    <socket-binding-group>  
      ...  
      <socket-binding name="jnp" port="5599" interface="jnp"/>  
      <socket-binding name="rmi-jnp" port="1099" interface="jnp"/>  
      <socket-binding name="legacy-remoting" port="4873"/>  
      ...  
    </socket-binding-group>  
    
Build and Installation : 
Define the environment variable for JBOSS_HOME pointing towards your EAP6 instalaltion.
Run deploy.sh
Define your configuration.

