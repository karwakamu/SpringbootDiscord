package discord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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

public class DiscordClient {
    private static final Logger log = LoggerFactory.getLogger(AppController.class);

    String token = "NTQ1OTUxNjYxNjcyMzY2MTEw.D3gcKQ.QL1qxBbdREraywJi188IAucLAe4";

    public DiscordClient()
    {

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

    public void Connect()
    {

        String url = GetWebsocketURL() + "/?v=6&encoding=json";
        
        WebSocketConnectionManager connectionManager = new WebSocketConnectionManager(new StandardWebSocketClient(), new DiscordWebSocketHandler(), url);
        connectionManager.start();

            // URI end = new URI("wss://gateway.discord.gg/?v=6&encoding=json");
            // WebSocketContainer container = ContainerProvider
            //         .getWebSocketContainer();
            // container.connectToServer(this, end);

            // JsonObject properties = Json.createObjectBuilder()
            //     .add("os", "linux")
            //     .add("broser", "java-discord")
            //     .add("device", "java-discord")
            // .build();

            // JsonObject payload = Json.createObjectBuilder()
            //     .add("token", token)
            //     .add("properties",properties)
            //     .add("compress",false)
            // .build();

            // JsonObject identify = Json.createObjectBuilder()
            //     .add("op", 2)
            //     .add("d", payload)
            // .build();
    }

    private class DiscordWebSocketHandler extends TextWebSocketHandler {

        @Override
        public void handleTextMessage(WebSocketSession session, TextMessage message) {
            log.info("Message Received [" + message.getPayload() + "]");
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            log.info("Connected");
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            log.error("Transport Error", exception);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status){
            log.info("Connection Closed [" + status.getReason() + "]");
        }
    }
}