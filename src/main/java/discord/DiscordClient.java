package discord;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import rx.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscordClient {
    private static final Logger log = LoggerFactory.getLogger(DiscordClient.class);
    private String GuildID;

    private PublishSubject<JSONObject> WriteMessageSubject;
    private PublishSubject<JSONObject> AddChannelSubject;
    private PublishSubject<JSONObject> DeleteChannelSubject;
    private PublishSubject<JSONArray> WritebulkMessageSubject;
    private DiscordWebSocketHandler socketHandler;
    private String token = "NTQ1OTUxNjYxNjcyMzY2MTEw.D3gcKQ.QL1qxBbdREraywJi188IAucLAe4";

    public DiscordClient() {
        AddChannelSubject = PublishSubject.create();
        DeleteChannelSubject = PublishSubject.create();
        WriteMessageSubject = PublishSubject.create();
        WritebulkMessageSubject = PublishSubject.create();
    }

    public void SetGuilID(String id) {
        GuildID = id;
    }

    public String GetToken() {
        return token;
    }

    public PublishSubject<JSONObject> GetAddChannelSubject() {
        return AddChannelSubject;
    }

    public PublishSubject<JSONObject> GetDeleteChannelSubject() {
        return DeleteChannelSubject;
    }

    public PublishSubject<JSONObject> GetWriteMessageSubject() {
        return WriteMessageSubject;
    }

    public PublishSubject<JSONArray> GetWritebulkMessageSubject() {
        return WritebulkMessageSubject;
    }

    public void WriteMessage(JSONObject payload) throws JSONException {
        JSONObject author = payload.getJSONObject("author");
        JSONObject message = new JSONObject();

        message.put("username", author.getString("username"));
        message.put("channel_id", payload.getString("channel_id"));
        message.put("content", payload.getString("content"));

        WriteMessageSubject.onNext(message);
    }

    public void WritebulkMessage(JSONArray JSONmessages) throws JSONException {
        JSONArray messages = new JSONArray();
        for (int i = JSONmessages.length() - 1; i >= 0; i--) {
            JSONObject message = new JSONObject();
            JSONObject obj = JSONmessages.optJSONObject(i);
            message.put("username", obj.getJSONObject("author").getString("username"));
            message.put("channel_id", obj.getString("channel_id"));
            message.put("content", obj.getString("content"));
            messages.put(message);
        }
        WritebulkMessageSubject.onNext(messages);
    }

    public void AddChannel(JSONObject payload) throws JSONException {
        if (payload.getInt("type") != 0)
            return;

        JSONObject channel = new JSONObject();

        channel.put("id", payload.getString("id"));
        channel.put("name", payload.getString("name"));
        AddChannelSubject.onNext(channel);
    }

    public void DeleteChannel(JSONObject payload) throws JSONException {
        JSONObject channel = new JSONObject();

        channel.put("id", payload.getString("id"));
        channel.put("name", payload.getString("name"));
        DeleteChannelSubject.onNext(channel);
    }

    public void GetChannels() throws Exception {
        URL url = new URL("https://discordapp.com/api/guilds/" + GuildID + "/channels");
        String content =  HttpGET(url);
        JSONArray JSONchannels = new JSONArray(content);

        for (int i = 0; i < JSONchannels.length(); i++) {
            AddChannel(JSONchannels.optJSONObject(i));
        }
    }

    public void GetMessageHistory(String ChannelID) throws Exception {
        if (ChannelID.isEmpty() || ChannelID.equals(""))
            return;

        URL url = new URL("https://discordapp.com/api/channels/" + ChannelID + "/messages");
        String content =  HttpGET(url);
        JSONArray JSONmessages = new JSONArray(content);

        WritebulkMessage(JSONmessages);
    }

    private String GetWebsocketURL() throws Exception {
        URL url = new URL("https://discordapp.com/api/gateway");
        String content =  HttpGET(url);
        JSONObject json = new JSONObject(content);
        return json.getString("url");
    }

    public void Connect() throws Exception {
        String url = GetWebsocketURL() + "/?v=6&encoding=json";
        socketHandler = new DiscordWebSocketHandler(this);

        WebSocketConnectionManager connectionManager = new WebSocketConnectionManager(new StandardWebSocketClient(),
                socketHandler, url);
        connectionManager.start();
    }

    public void SendMessage(JSONObject JSONmessage) throws Exception {
        String channel = JSONmessage.getString("channel_id");
        if (channel.isEmpty() || channel.equals(""))
            return;

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

    private String HttpGET(URL url) throws Exception
    {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "vittu");
        con.setRequestProperty("Authorization", "Bot " + token);
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null)
        {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();

        return content.toString();
    }
}