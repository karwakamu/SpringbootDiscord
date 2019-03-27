package discord;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class AppController
{
    private static final Logger log = LoggerFactory.getLogger(AppController.class);
    private DiscordClient discordClient;

    public AppController()
    {
        discordClient = new DiscordClient();

        try
        {
            discordClient.Connect();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/")
    public String getIndex(Model model) throws Exception
    {
        return "chat.html";
    }

    @GetMapping("api/channel/get")
    public ResponseEntity<String> getChannels() throws Exception
    {
        JSONArray channels = discordClient.GetChannels();

        return ResponseEntity.status(HttpStatus.OK).body(channels.toString());
    }

    @GetMapping("api/channel/add")
    public SseEmitter getAddChannel()
    {
        SubscribedEmitter emitter = new SubscribedEmitter(1440000L);
        emitter.SetSubscription(discordClient.GetAddChannelSubject().subscribe(value -> sendJSON(emitter, value),
        emitter::completeWithError,
        emitter::complete));
        return emitter;
    }

    @GetMapping("api/channel/delete")
    public SseEmitter getDeleteChannel()
    {
        SubscribedEmitter emitter = new SubscribedEmitter(1440000L);
        emitter.SetSubscription(discordClient.GetDeleteChannelSubject().subscribe(value -> sendJSON(emitter, value),
        emitter::completeWithError,
        emitter::complete));
        return emitter;
    }

    @GetMapping("api/message/write")
    public SseEmitter getWriteMessage()
    {
        SubscribedEmitter emitter = new SubscribedEmitter(1440000L);
        emitter.SetSubscription(discordClient.GetWriteMessageSubject().subscribe(value -> sendJSON(emitter, value),
        emitter::completeWithError,
        emitter::complete));
        return emitter;
    }

    @PostMapping("api/message/send")
    public void postSendMessage(@RequestParam Map<String, String> messageMap) throws Exception
    {
        JSONObject JSONmessage = new JSONObject();

        for (Map.Entry<String, String> pair : messageMap.entrySet())
        {
            JSONmessage.put(pair.getKey(), pair.getValue());
        }

        discordClient.SendMessage(JSONmessage);
    }

    @PostMapping("api/message/history")
    public ResponseEntity<String> postMessageHistory(@RequestParam Map<String, String> channelMap) throws Exception
    {
        JSONObject JSONmessage = new JSONObject();

        for (Map.Entry<String, String> pair : channelMap.entrySet())
        {
            JSONmessage.put(pair.getKey(), pair.getValue());
        }

        JSONArray history = discordClient.GetMessageHistory(JSONmessage.getString("channel_id"));

        return ResponseEntity.status(HttpStatus.OK).body(history.toString());
    }

    private void sendJSON(SubscribedEmitter emitter, JSONObject message)
    {
        try
        {
            emitter.send(message.toString());
        }
        catch (Exception ex)
        {
            emitter.Unsubscribe();
            emitter.completeWithError(ex);
        }
    }
}