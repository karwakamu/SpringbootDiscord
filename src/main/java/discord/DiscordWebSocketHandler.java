package discord;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class DiscordWebSocketHandler extends TextWebSocketHandler
{
    private static final Logger log = LoggerFactory.getLogger(DiscordWebSocketHandler.class);
    private DiscordClient discordClient;
    private WebSocketSession session;
    private Timer heartbeatTimer;
    private int seq;
    private String sessionID;

    public DiscordWebSocketHandler(DiscordClient client)
    {
        this.discordClient = client;
    }

    private void sendHeartbeat() throws Exception
    {
        JSONObject heartbeatJSON = new JSONObject();
        heartbeatJSON.put("op", 1);
        heartbeatJSON.put("d",seq);
        sendJSON(heartbeatJSON);
    }

    public void sendJSON(JSONObject json) throws IOException
    {
        session.sendMessage(new TextMessage(json.toString()));
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception
    {
        JSONObject JSONmessage = new JSONObject();
        JSONObject JSONpayload = new JSONObject();

        JSONmessage = new JSONObject(message.getPayload());

        Object payloadOBJ = JSONmessage.get("d");
        if(payloadOBJ != JSONObject.NULL) JSONpayload = new JSONObject(payloadOBJ.toString());
        
        int op = JSONmessage.getInt("op");

        switch(op)
        {
            case 0:
                String t = JSONmessage.getString("t");
                switch(t)
                {
                    case "READY":
                        sessionID = JSONpayload.getString("session_id");
                        break;

                    case "MESSAGE_CREATE":
                        discordClient.WriteMessage(JSONpayload);
                        break;

                    case "CHANNEL_CREATE":
                        discordClient.AddChannel(JSONpayload);
                        break;

                    case "CHANNEL_DELETE":
                        discordClient.DeleteChannel(JSONpayload);
                        break;

                    case "GUILD_CREATE":
                        discordClient.SetGuilID(JSONpayload.getString("id"));
                        break;

                }       
                break;

            case 10:
                int interval = JSONpayload.getInt("heartbeat_interval");
                heartbeatTimer = new Timer();
                heartbeatTimer.scheduleAtFixedRate(new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            sendHeartbeat();
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                },0,interval);
                break;

            case 1:
                sendHeartbeat();
                break;

            case 7:
                session.close();
                discordClient.Connect();
                break;
        }

        Object obj = JSONmessage.get("s");
        if (obj != JSONObject.NULL) seq = (int)obj;
    }

    private void identify() throws Exception
    {
        JSONObject identifyJSON = new JSONObject();
        JSONObject payloadJSON = new JSONObject();
        JSONObject propertiesJSON = new JSONObject();

        propertiesJSON.put("$os","");
        propertiesJSON.put("$browser","disco");
        propertiesJSON.put("$device","disco");

        payloadJSON.put("token", discordClient.GetToken());
        payloadJSON.put("properties",propertiesJSON);
        payloadJSON.put("compress",false);

        identifyJSON.put("op",2);
        identifyJSON.put("d",payloadJSON);

        sendJSON(identifyJSON);
    }

    private void resume() throws Exception
    {
        JSONObject resumeJSON = new JSONObject();
        resumeJSON.put("token", discordClient.GetToken());
        resumeJSON.put("session_id", sessionID);
        resumeJSON.put("seq", seq);
        sendJSON(resumeJSON);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception
    {
        log.info("Websocket Connected");
        this.session = session;
        identify();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception)
    {
        log.error("Websocket Transport Error", exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception
    {
        session.close();
        log.info("Websocket Connection Closed [" + status.getReason() + "]");
        try
        {
            resume();
        }
        catch (Exception e)
        {
            discordClient.Connect();
        }
    }
}