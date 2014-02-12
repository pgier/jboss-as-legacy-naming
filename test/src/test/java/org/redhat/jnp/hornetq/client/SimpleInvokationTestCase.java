/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.redhat.jnp.hornetq.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.hamcrest.Matchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redhat.jnp.hornetq.mdb.SimpleMDB;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
@RunWith(Arquillian.class)
@ServerSetup({SimpleInvokationTestCase.JmsQueueSetup.class})
@RunAsClient
public class SimpleInvokationTestCase {
    
    private static final Logger log = Logger.getLogger(SimpleInvokationTestCase.class.getName());

    private static final String QUEUE_JNDI_NAME = "jms/queue/eap6Queue";
    private static final String REPLY_QUEUE_JNDI_NAME = "jms/queue/eap6ReplyQueue";
    private static final String DEFAULT_CONNECTION_FACTORY = "java:jboss/exported/jms/RemoteConnectionFactory";
    private static final String JNDI_CONFIG = "jndi-eap6.properties";
    
    private InitialContext getInitialContext() throws NamingException, IOException {
        Properties jndiProperties = new Properties();
        jndiProperties.load(this.getClass().getClassLoader().getResourceAsStream(System.getProperty("jndi_config", JNDI_CONFIG)));
        return new javax.naming.InitialContext(jndiProperties);
    }
    
    @Deployment
    public static Archive createDeployment() {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ping-pong.jar");
        jar.addClasses(SimpleMDB.class, JmsQueueSetup.class);
        jar.addPackage(JMSOperations.class.getPackage());
        return jar;
    }
    
    @Test
    public void testSendMessages() throws Exception {
        List<String> responses = sendMessage("Hello World!", "Bye bye world!");
        Assert.assertThat(responses, Matchers.is(Matchers.notNullValue()));
        
    }

    private List<String> sendMessage(String... messages) throws Exception {
        InitialContext initialContext = getInitialContext();
        Connection connection = null;
        List<String>  responses = new ArrayList<String>(messages.length);
        try {
            // Step 1. Perfom a lookup on the queue
            Destination destination = lookupDestination(initialContext);

            Destination target = lookupReceiver(initialContext);
            // Step 2. Perform a lookup on the Connection Factory
            ConnectionFactory connectionFactory = lookupConnectionFactory(initialContext);
            // Step 3. Open a connection
            connection = connectionFactory.createConnection("quickstartUser", "quickstartPwd1!");
            // Step 4. Open a session
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            // Step 5. Create a producer
            MessageProducer producer = session.createProducer(destination);
            MessageConsumer consumer = session.createConsumer(target);
            log.log(Level.INFO, "Sending {0} messages", messages.length);

            connection.start();

            // Send the specified number of messages
            for (String content : messages) {
                TextMessage message = session.createTextMessage(content);
                producer.send(message);
                log.log(Level.INFO, "Sent message: {0}", message.getText());
            }
            while (true) {
                TextMessage message = (TextMessage) consumer.receive(1000);
                if (message != null) {
                    System.out.println("Reading message: " + message.getText());
                    responses.add( message.getText());
                } else {
                    break;
                }
            }
            return responses;
        } catch (Exception e) {
            log.severe(e.getMessage());
            throw e;
        } finally {
            // Step 12. Be sure to close our JMS resources!
            if (initialContext != null) {
                initialContext.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

    private ConnectionFactory lookupConnectionFactory(InitialContext initialContext) throws NamingException {
        String connectionFactoryString = System.getProperty("connection.factory", DEFAULT_CONNECTION_FACTORY);
        log.log(Level.INFO, "Attempting to acquire connection factory \"{0}\"", connectionFactoryString);
        ConnectionFactory connectionFactory = (ConnectionFactory) initialContext.lookup(connectionFactoryString);
        log.log(Level.INFO, "Found connection factory \"{0}\" in JNDI", connectionFactoryString);
        return connectionFactory;
    }

    private Destination lookupDestination(InitialContext initialContext) throws NamingException {
        String destinationString = System.getProperty("destination", QUEUE_JNDI_NAME);
        log.log(Level.INFO, "Attempting to acquire destination \"{0}\"", destinationString);
        Destination destination = (Destination) initialContext.lookup(destinationString);
        log.log(Level.INFO, "Found destination \"{0}\" in JNDI", destinationString);
        return destination;
    }

    private Destination lookupReceiver(InitialContext initialContext) throws NamingException {
        String destinationString = System.getProperty("destination", REPLY_QUEUE_JNDI_NAME);
        log.log(Level.INFO, "Attempting to acquire destination \"{0}\"", destinationString);
        Destination destination = (Destination) initialContext.lookup(destinationString);
        log.log(Level.INFO, "Found destination \"{0}\" in JNDI", destinationString);
        return destination;
    }

    static class JmsQueueSetup implements ServerSetupTask {

        private JMSOperations jmsAdminOperations;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient);
            jmsAdminOperations.createJmsQueue("eap6Queue", QUEUE_JNDI_NAME);
            jmsAdminOperations.createJmsQueue("eap6ReplyQueue", REPLY_QUEUE_JNDI_NAME);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (jmsAdminOperations != null) {
                jmsAdminOperations.removeJmsQueue("eap6ReplyQueue");
                jmsAdminOperations.removeJmsQueue("eap6Queue");
                jmsAdminOperations.close();
            }
        }
    }

}
