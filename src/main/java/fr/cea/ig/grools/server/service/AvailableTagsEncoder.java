package fr.cea.ig.grools.server.service;


import fr.cea.ig.grools.server.AvailableTags;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import org.apache.log4j.Logger;


public class AvailableTagsEncoder implements Encoder.Text<AvailableTags>{
    private static final Logger LOG = Logger.getLogger(AvailableTagsEncoder.class);

    @Override
    public String encode(AvailableTags object) throws EncodeException {
        LOG.info("Encode:"+ object.toString());
        return object.toString();
    }

    @Override
    public void init(EndpointConfig config) {
        LOG.info("init called on AvailableTagsEncoder");
    }

    @Override
    public void destroy() {
        LOG.info("destroy called on AvailableTagsEncoder");
    }
}
