package discord;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import rx.Subscription;
import java.util.Map;
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

    @GetMapping("api/init")
    public void getInit() throws Exception
    {
        discordClient.GetChannels();
    }

    @GetMapping("api/channel/add")
    public SseEmitter getChannelCreate()
    {
        SubscribedEmitter emitter = new SubscribedEmitter(1440000L);
        emitter.SetSubscription(discordClient.GetAddChannelSubject().subscribe(value -> notifyProgress(emitter, value),
        emitter::completeWithError,
        emitter::complete));
        return emitter;
    }

    @GetMapping("api/channel/delete")
    public SseEmitter getChannelDelete()
    {
        SubscribedEmitter emitter = new SubscribedEmitter(1440000L);
        emitter.SetSubscription(discordClient.GetDeleteChannelSubject().subscribe(value -> notifyProgress(emitter, value),
        emitter::completeWithError,
        emitter::complete));
        return emitter;
    }

    @GetMapping("api/message/receive")
    public SseEmitter getMessageReceive()
    {
        SubscribedEmitter emitter = new SubscribedEmitter(1440000L);
        emitter.SetSubscription(discordClient.GetReceiveMessageSubject().subscribe(value -> notifyProgress(emitter, value),
        emitter::completeWithError,
        emitter::complete));
        return emitter;
    }

    @PostMapping("api/message/send")
    public void postMessage(@RequestParam Map<String, String> messageMap) throws Exception
    {
        JSONObject JSONmessage = new JSONObject();

        for (Map.Entry<String, String> pair : messageMap.entrySet())
        {
            JSONmessage.put(pair.getKey(), pair.getValue());
        }

        discordClient.SendMessage(JSONmessage);
    }

    private void notifyProgress(SubscribedEmitter emitter, JSONObject message)
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