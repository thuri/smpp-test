package de.telekom.michael.lueck.camel.smpp;

import java.io.File;
import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smpp.smscsim.DeliveryInfoSender;
import org.smpp.smscsim.PDUProcessorGroup;
import org.smpp.smscsim.SMSCListenerImpl;
import org.smpp.smscsim.ShortMessageStore;
import org.smpp.smscsim.SimulatorPDUProcessorFactory;
import org.smpp.smscsim.util.Table;

public class Main {

  private static final Logger logger = LogManager.getLogger();
  
  public static void main(String[] args) throws Exception {

    logger.always().log("Starting SMSC");
    
    final var smsc = new SMSCListenerImpl(12345, true);
    final var deliveryInfoSender = new DeliveryInfoSender();
    deliveryInfoSender.start();
    final var factory = new SimulatorPDUProcessorFactory(
        new PDUProcessorGroup(), 
        new ShortMessageStore(), 
        deliveryInfoSender, 
        new Table(new File("src/main/resources/users.txt").getAbsolutePath()));
    factory.setDisplayInfo(true);
    smsc.setPDUProcessorFactory(factory);
    smsc.setAcceptTimeout(500);
    smsc.start();
    
    try(var context = new DefaultCamelContext()) {

      context.addRoutes(new RouteBuilder() {
        @Override
        public void configure() throws Exception {
          from("direct:test")
          .to("smpp://smpptest@localhost:12345"
              + "?maxReconnect=2"
              + "&reconnectDelay=500"
              + "&initialReconnectDelay=500"
              + "&systemId=smpptest"
              + "&lazySessionCreation=true"
          );
        }
      });
      
      context.start();
      
      Map<String, Object> credentials = Map.of(
//          SmppConstants.SYSTEM_ID, "smpptest", 
//          SmppConstants.PASSWORD, "smpptest"
      );
      context.createProducerTemplate().sendBodyAndHeaders("direct:test", "TestSMS", credentials);
      
      System.in.read();
      context.stop();
      
    } finally {
      smsc.stop();
    }
  }
}
