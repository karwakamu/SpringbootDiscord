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
    public String getIndex(Model model)
    {
        String str ="";
        for (Map.Entry<String, String> pair : discordClient.GetChannels().entrySet())
        {
            str += "<button onclick=\"setChannel(this)\" value=\""+ pair.getKey() +"\">" + pair.getValue() + "</button><br/>";
        }
        model.addAttribute("channels", str);
        return "chat";
    }

    private void notifyProgress(CustomEmitter emitter, JSONObject message)
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
    
    private class CustomEmitter extends SseEmitter
    {
        private Subscription subscription;

        public CustomEmitter(long timeout)
        {
            super(timeout);
        }

        public void SetSubscription(Subscription subscription)
        {
            this.subscription = subscription;
        }

        public void Unsubscribe()
        {
            subscription.unsubscribe();
        }
    }

    @GetMapping("api/chat")
    public SseEmitter streamChat()
    {
        CustomEmitter emitter = new CustomEmitter(1440000L);
        emitter.SetSubscription(discordClient.GetSubject().subscribe(value -> notifyProgress(emitter, value),
        emitter::completeWithError,
        emitter::complete));
        return emitter;
    }

    @RestController
    private class CustomRestController
    {
        @PostMapping("api/message")
        public void postMessage(@RequestParam Map<String, String> messageMap) throws Exception
        {
            JSONObject JSONmessage = new JSONObject();

            for (Map.Entry<String, String> pair : messageMap.entrySet())
            {
                JSONmessage.put(pair.getKey(), pair.getValue());
            }

            discordClient.SendMessage(JSONmessage);
        }
    }
}