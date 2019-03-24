package discord;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
public class AppController
{
    private DiscordClient discordClient;

    public AppController()
    {
        discordClient = new DiscordClient();

        try
        {
            discordClient.Connect();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @GetMapping("/default")
    public String defaultGet()
    {
        return "default";
    }

    private void notifyProgress(SseEmitter emitter, String str)
    {
        try
        {
            emitter.send(str);
        }
        catch (Exception ex) {
            emitter.completeWithError(ex);
        }
    }

    @GetMapping("/sse")
    public SseEmitter streamSseMvc()
    {
        SseEmitter emitter = new SseEmitter(1440000L);
        discordClient.GetSubject().subscribe(value -> notifyProgress(emitter, value),
        emitter::completeWithError,
        emitter::complete);
        return emitter;
    }
}