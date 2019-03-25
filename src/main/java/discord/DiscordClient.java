package discord;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import rx.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscordClient {
    private static final Logger log = LoggerFactory.getLogger(DiscordClient.class);
    private PublishSubject<JSONObject> subject;
    private DiscordWebSocketHandler socketHandler;
    private Map<String, String> channels;
    private String token = "NTQ1OTUxNjYxNjcyMzY2MTEw.D3gcKQ.QL1qxBbdREraywJi188IAucLAe4";

    public DiscordClient() {
        channels = new HashMap<String, String>();
        subject = PublishSubject.create();
    }

    public String GetToken()
    {
        return token;
    }

    public Map<String, String> GetChannels() {
        return channels;
    }

    public PublishSubject<JSONObject> GetSubject() {
        return subject;
    }

    public void ReceivedMessage(JSONObject message) {
        subject.onNext(message);
    }

    private String GetWebsocketURL() throws Exception {
        URL url = new URL("https://discordapp.com/api/gateway");

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

        JSONObject json = new JSONObject(content.toString());
        return json.getString("url");
    }

    public void Connect() throws Exception {
        String url = GetWebsocketURL() + "/?v=6&encoding=json";
        socketHandler = new DiscordWebSocketHandler(this);

        WebSocketConnectionManager connectionManager = new WebSocketConnectionManager(new StandardWebSocketClient(),
                socketHandler, url);
        connectionManager.start();
    }

    public void SendMessage(JSONObject JSONmessage) throws Exception
    {
        String channel = JSONmessage.getString("channel_id");
        if(channel.isEmpty() || channel.equals("")) return;

        JSONmessage.put("tts", false);
        URL url = new URL("https://discordapp.com/api/channels/" + channel + "/messages");

        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", "vittu");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("Authorization", "Bot " + token);
        con.setDoOutput(true);
        con.setDoInput(true);

        OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream(), "utf-8");
        wr.write(JSONmessage.toString());
        wr.flush();
        
        con.getResponseCode(); 
        con.disconnect();
    }
}