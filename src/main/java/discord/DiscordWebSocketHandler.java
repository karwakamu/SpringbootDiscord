package discord;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import discord.DiscordMessage;

public class DiscordWebSocketHandler extends TextWebSocketHandler
{
    private static final Logger log = LoggerFactory.getLogger(DiscordWebSocketHandler.class);
    private DiscordClient discordClient;
    private WebSocketSession session;
    private Timer heartbeatTimer;
    private Timer resumeTimer;
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
        if(session.isOpen())
        {
            session.sendMessage(new TextMessage(json.toString()));
        }    
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
                        JSONObject JSONuser = JSONpayload.getJSONObject("author");
                        String user = JSONuser.getString("username");
                        String channel = JSONpayload.getString("channel_id");
                        String content = JSONpayload.getString("content");
                        DiscordMessage discordMessage = new DiscordMessage(channel,user,content);
                        discordClient.ReceivedMessage(discordMessage);
                        break;

                    case "GUILD_CREATE":
                        JSONArray JSONchannels = JSONpayload.getJSONArray("channels");
                        
                        for(int i = 0; i < JSONchannels.length(); i++)
                        {
                            JSONObject JSONchannel = (JSONObject)JSONchannels.get(i);
                            if(JSONchannel.getInt("type") == 0)
                            {
                                String id = JSONchannel.getString("id");
                                String name = JSONchannel.getString("name");
                                discordClient.GetChannels().put(id, name);
                            }
                        }
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
        if(resumeTimer != null) resumeTimer.cancel();
        identify();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception)
    {
        log.error("Websocket Transport Error", exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status)
    {
        log.info("Websocket Connection Closed [" + status.getReason() + "]");
        resumeTimer = new Timer();
        resumeTimer.scheduleAtFixedRate(new TimerTask()
            {
                @Override
                public void run()
                {
                    try
                    {
                        resume();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            },0,5000);
    }
}