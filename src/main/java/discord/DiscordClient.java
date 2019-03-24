package discord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import rx.subjects.PublishSubject;

public class DiscordClient {

    private static final Logger log = LoggerFactory.getLogger(AppController.class);
    private PublishSubject<DiscordMessage> subject;
    public Map<String, String> channels;
    private String token = "NTQ1OTUxNjYxNjcyMzY2MTEw.D3gcKQ.QL1qxBbdREraywJi188IAucLAe4";

    public class DiscordMessage
    {
        public String channel;
        public String user;
        public String content;

        public DiscordMessage(String channel, String user, String content)
        {
            this.channel = channel;
            this.user = user;
            this.content = content;
        }
    }

    public DiscordClient()
    {
        channels = new HashMap<String,String>();
        subject = PublishSubject.create();
    }
    
    public PublishSubject<DiscordMessage> GetSubject()
    {
        return subject;
    }

    private String GetWebsocketURL()
    {
        String wsURL ="";

        try
        {
            URL url = new URL("https://discordapp.com/api/gateway");

            try
            {
                HttpURLConnection con = (HttpURLConnection) url.openConnection();

                con.setRequestMethod("GET");
                con.setRequestProperty("User-Agent", "vittu");
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer content = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();

                con.disconnect();

                try
                {
                    JSONObject json = new JSONObject(content.toString());
                    wsURL = json.get("url").toString();
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
        catch (MalformedURLException ex)
        {
            ex.printStackTrace();
        }

        return wsURL;
    }

    public void Connect() throws Exception
    {
        String url = GetWebsocketURL() + "/?v=6&encoding=json";
        
        WebSocketConnectionManager connectionManager = new WebSocketConnectionManager(new StandardWebSocketClient(), new DiscordWebSocketHandler(), url);
        connectionManager.start();
    }

    private class DiscordWebSocketHandler extends TextWebSocketHandler
    {
        WebSocketSession session;
        Timer heartbeatTimer;
        Timer resumeTimer;
        int seq;
        String sessionID;

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
                            subject.onNext(discordMessage);
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
                                    channels.put(id, name);
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
                    Connect();
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

            payloadJSON.put("token",token);
            payloadJSON.put("properties",propertiesJSON);
            payloadJSON.put("compress",false);

            identifyJSON.put("op",2);
            identifyJSON.put("d",payloadJSON);

            sendJSON(identifyJSON);
        }

        private void resume() throws Exception
        {
            JSONObject resumeJSON = new JSONObject();
            resumeJSON.put("token", token);
            resumeJSON.put("session_id", sessionID);
            resumeJSON.put("seq", seq);
            sendJSON(resumeJSON);
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception
        {
            log.info("Connected");
            this.session = session;
            if(resumeTimer != null) resumeTimer.cancel();
            identify();
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception)
        {
            log.error("Transport Error", exception);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status)
        {
            log.info("Connection Closed [" + status.getReason() + "]");
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
}