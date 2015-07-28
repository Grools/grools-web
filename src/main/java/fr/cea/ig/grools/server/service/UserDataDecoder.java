package fr.cea.ig.grools.server.service;

import fr.cea.ig.grools.server.UserData;
import org.apache.log4j.Logger;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import java.io.Reader;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class UserDataDecoder implements Decoder.Text<UserData> {
    private static final Logger LOG = Logger.getLogger(UserDataDecoder.class);

    @Override
    public UserData decode(final String s) throws DecodeException {
        LOG.info("Decode: "+s);
        Reader reader           = new StringReader(s);
        JsonReader jsonReader   = Json.createReader(reader);
        JsonObject object       = jsonReader.readObject();
        return new UserData( object.getString("species"), object.getBoolean("speciesSelected"), object.getString("strains"), object.getBoolean("strainsSelected") );
    }

    @Override
    public boolean willDecode(final String s) {
        LOG.info("Will decode asked for " + s);
        try {
            // Check if incoming message is valid JSON
            Json.createReader(new StringReader(s)).readObject();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void init(final EndpointConfig config) {
        LOG.info("init called on UserDataDecoder");
    }

    @Override
    public void destroy() {
        LOG.info("destroy called on UserDataDecoder");
    }
}
