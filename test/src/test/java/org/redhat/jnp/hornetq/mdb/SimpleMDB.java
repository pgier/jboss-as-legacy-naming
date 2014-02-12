/*
 * Copyright (C) 2013 Red Hat, inc., and individual contributors
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
package org.redhat.jnp.hornetq.mdb;

import javax.annotation.Resource;
import javax.jms.Message;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
@MessageDriven(name = "SimpleMDB", activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/queue/eap6Queue")
})

public class SimpleMDB implements MessageListener {

    @Resource(mappedName = "java:/ConnectionFactory")
    private ConnectionFactory cf;
    @Resource(mappedName = "java:/jms/queue/eap6ReplyQueue")
    private Queue targetQueue;

    public SimpleMDB() {
    }

    @Override
    public void onMessage(Message message) {
	try {
	    //We know the client is sending a text message so we cast
	    TextMessage textMessage = (TextMessage) message;
	    // Get the text from the message.
	    String text = textMessage.getText();
	    System.out.println("message " + text + " received");
	    Connection con = null;
	    Session ses = null;
	    MessageProducer sender = null;
	    try {
		con = cf.createConnection();
		ses = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
		sender = ses.createProducer(targetQueue);
		TextMessage msg = ses.createTextMessage(text);
		sender.send(msg);
		System.out.println("============== sendTextMessageToQueue succeeded!!!!!");
	    } catch (JMSException e) {
		throw new RuntimeException("sendTextMessageToQueue failed", e);
	    } finally {
		close(sender, ses, con);
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    private void close(MessageProducer sender, Session session, Connection conn) {

	if (sender != null) {
	    try {
		System.out.println("============= closing MessageProducer ");
		sender.close();
	    } catch (Exception ignore) {
		System.out.println("============= ...but it failed!");
	    }
	}
	if (session != null) {
	    try {
		System.out.println("============= closing Session ");
		session.close();
	    } catch (Exception ignore) {
		System.out.println("============= ...but it failed!");
	    }
	}
	if (conn != null) {
	    try {
		System.out.println("============= closing Connection ");
		conn.close();
	    } catch (Exception ignore) {
		System.out.println("============= ...but it failed!");
	    }
	}
    }

}
