package discord;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import discord.DiscordMessage;
import javax.servlet.http.HttpSession;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class AppController {
    private static final Logger log = LoggerFactory.getLogger(AppController.class);
    private DiscordClient discordClient;

    public AppController() {
        discordClient = new DiscordClient();

        try {
            discordClient.Connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/index")
    public String getIndex(Model model) {
        String str ="";
        for (Map.Entry<String, String> pair : discordClient.GetChannels().entrySet())
        {
            str += "<a href=\"/chat?channel=" + pair.getKey() + "\">" + pair.getValue()  + "</a><br/>";
        }
        model.addAttribute("channels",str);
        return "index";
    }

    @GetMapping("/chat")
    public String defaultGet(@RequestParam(name="channel", required=false, defaultValue="") String channel, HttpSession session)
    {
        if(!channel.equals(""))session.setAttribute("channel", channel);
        return "chat";
    }

    private void notifyProgress(CustomEmitter emitter, DiscordMessage message)
    {
        try
        {
            if(emitter.channel.equals(message.GetChannel()))
            {
                String str = "<" + message.GetUser() + ">    " + message.GetContent();
                emitter.send(str);
            }
        }
        catch (Exception ex)
        {
            emitter.completeWithError(ex);
        }
    }

    private class CustomEmitter extends SseEmitter
    {
        public String channel;

        public CustomEmitter(long timeout)
        {
            super(timeout);
        }
    }

    @GetMapping("/chatSSE")
    public SseEmitter streamChat(HttpSession session)
    {
        CustomEmitter emitter = new CustomEmitter(1440000L);
        emitter.channel = session.getAttribute("channel").toString();
        discordClient.GetSubject().subscribe(value -> notifyProgress(emitter, value),
        emitter::completeWithError,
        emitter::complete);
        return emitter;
    }

    @PostMapping("/chat")
    public void postMessage(@RequestParam(name="message", required=false, defaultValue="") String message, HttpSession session) throws Exception
    {
        log.info(message);
        log.info(session.getAttribute("channel").toString());
        discordClient.SendMessage(message, session.getAttribute("channel").toString());
    }
}